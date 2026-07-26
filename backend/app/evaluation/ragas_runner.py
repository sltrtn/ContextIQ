"""RAG evaluation pipeline — runs questions against a pipeline config and computes metrics.

Uses LLM-as-judge for faithfulness, answer relevancy, context precision, and context recall.
No external evaluation framework dependency — full control over methodology.
"""

import json
import time
from pathlib import Path
from dataclasses import dataclass, field, asdict

from app.core.llm import get_llm


@dataclass
class EvalResult:
    question_id: str
    question: str
    answer: str
    contexts: list[str]
    ground_truth: str
    faithfulness: float = 0.0
    answer_relevancy: float = 0.0
    context_precision: float = 0.0
    context_recall: float = 0.0
    latency_ms: float = 0.0
    error: str | None = None


@dataclass
class EvalSummary:
    config: str
    num_questions: int
    faithfulness: float = 0.0
    answer_relevancy: float = 0.0
    context_precision: float = 0.0
    context_recall: float = 0.0
    latency_p50_ms: float = 0.0
    latency_p95_ms: float = 0.0
    results: list[EvalResult] = field(default_factory=list)


def load_test_set(path: str = "data/eval/test_set.json") -> list[dict]:
    """Load the evaluation test set."""
    test_path = Path(path)
    if not test_path.exists():
        # Try relative to backend/
        test_path = Path(__file__).resolve().parent.parent.parent.parent / path
    with open(test_path) as f:
        return json.load(f)


def _judge_score(llm, prompt: str) -> float:
    """Ask the LLM to score something on a 0-1 scale. Returns float."""
    try:
        response = llm.complete(prompt)
        text = str(response).strip()
        # Extract a number from the response
        for token in text.split():
            try:
                val = float(token)
                return max(0.0, min(1.0, val))
            except ValueError:
                continue
        # Try to find decimal in the response
        import re
        match = re.search(r'(\d+\.?\d*)', text)
        if match:
            val = float(match.group(1))
            if val > 1.0:
                val = val / 10.0 if val <= 10.0 else val / 100.0
            return max(0.0, min(1.0, val))
        return 0.0
    except Exception as e:
        print(f"  Judge error: {e}")
        return 0.0


def evaluate_faithfulness(llm, answer: str, contexts: list[str]) -> float:
    """Check if the answer is grounded in the provided contexts.

    For each claim in the answer, verify it's supported by at least one context chunk.
    Score = supported_claims / total_claims.
    """
    if not contexts:
        return 0.0

    context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c}" for i, c in enumerate(contexts[:5]))

    prompt = f"""You are evaluating whether an answer is faithful to the provided contexts.

CONTEXTS:
{context_block}

ANSWER:
{answer}

Task: List each distinct claim in the ANSWER. For each claim, state whether it is SUPPORTED or NOT SUPPORTED by the CONTEXTS. Then give a single score from 0.0 to 1.0 where:
- 1.0 = every claim is supported by the contexts
- 0.5 = about half the claims are supported
- 0.0 = no claims are supported

Format your response as:
Claims: [list]
Supported: [count]
Total: [count]
Score: [0.0-1.0]"""

    return _judge_score(llm, prompt)


def evaluate_answer_relevancy(llm, question: str, answer: str) -> float:
    """Check how relevant the answer is to the question."""
    prompt = f"""Rate how well this answer addresses the question on a scale of 0.0 to 1.0.

QUESTION: {question}

ANSWER: {answer}

Score:
- 1.0 = directly and completely answers the question
- 0.7 = mostly answers but misses some aspects
- 0.4 = partially relevant but doesn't fully address the question
- 0.1 = barely relevant or completely off-topic

Score: """

    return _judge_score(llm, prompt)


def evaluate_context_precision(llm, question: str, contexts: list[str]) -> float:
    """Check if the retrieved contexts are relevant to the question."""
    if not contexts:
        return 0.0

    context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c}" for i, c in enumerate(contexts[:5]))

    prompt = f"""Rate how relevant these retrieved contexts are to the question.

QUESTION: {question}

RETRIEVED CONTEXTS:
{context_block}

Score from 0.0 to 1.0:
- 1.0 = all contexts are directly relevant to the question
- 0.7 = most contexts are relevant
- 0.4 = about half are relevant
- 0.1 = few or no contexts are relevant

Score: """

    return _judge_score(llm, prompt)


def evaluate_context_recall(llm, ground_truth: str, contexts: list[str]) -> float:
    """Check if the contexts contain the information needed to answer the question."""
    if not contexts:
        return 0.0

    context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c}" for i, c in enumerate(contexts[:5]))

    prompt = f"""Rate how well these contexts cover the ground truth information.

GROUND TRUTH ANSWER:
{ground_truth}

RETRIEVED CONTEXTS:
{context_block}

Score from 0.0 to 1.0:
- 1.0 = contexts contain all information needed to produce the ground truth answer
- 0.7 = contexts contain most of the needed information
- 0.4 = contexts contain some but not enough
- 0.1 = contexts contain little to none of the needed information

Score: """

    return _judge_score(llm, prompt)


def run_evaluation(
    config_name: str,
    query_fn,
    test_set: list[dict] | None = None,
    test_set_path: str = "data/eval/test_set.json",
) -> EvalSummary:
    """Run evaluation for a given pipeline config.

    Args:
        config_name: Name of the configuration (e.g., "vector_only", "hybrid_rerank")
        query_fn: Function that takes a question string and returns (answer, contexts_list)
        test_set: Optional pre-loaded test set. If None, loads from file.
        test_set_path: Path to test set JSON.

    Returns:
        EvalSummary with per-question results and aggregate metrics.
    """
    if test_set is None:
        test_set = load_test_set(test_set_path)

    llm = get_llm()
    results = []

    for i, item in enumerate(test_set):
        print(f"  [{i+1}/{len(test_set)}] {item['id']}: {item['question'][:60]}...")
        start = time.time()

        try:
            answer, contexts = query_fn(item["question"])
            latency = (time.time() - start) * 1000

            faithfulness = evaluate_faithfulness(llm, answer, contexts)
            relevancy = evaluate_answer_relevancy(llm, item["question"], answer)
            precision = evaluate_context_precision(llm, item["question"], contexts)
            recall = evaluate_context_recall(llm, item["ground_truth"], contexts)

            result = EvalResult(
                question_id=item["id"],
                question=item["question"],
                answer=answer,
                contexts=contexts,
                ground_truth=item["ground_truth"],
                faithfulness=faithfulness,
                answer_relevancy=relevancy,
                context_precision=precision,
                context_recall=recall,
                latency_ms=latency,
            )
        except Exception as e:
            latency = (time.time() - start) * 1000
            result = EvalResult(
                question_id=item["id"],
                question=item["question"],
                answer="",
                contexts=[],
                ground_truth=item["ground_truth"],
                latency_ms=latency,
                error=str(e),
            )
            print(f"    ERROR: {e}")

        results.append(result)

    # Compute aggregates
    valid = [r for r in results if r.error is None]
    latencies = sorted([r.latency_ms for r in valid]) if valid else [0]

    summary = EvalSummary(
        config=config_name,
        num_questions=len(test_set),
        faithfulness=sum(r.faithfulness for r in valid) / len(valid) if valid else 0,
        answer_relevancy=sum(r.answer_relevancy for r in valid) / len(valid) if valid else 0,
        context_precision=sum(r.context_precision for r in valid) / len(valid) if valid else 0,
        context_recall=sum(r.context_recall for r in valid) / len(valid) if valid else 0,
        latency_p50_ms=latencies[len(latencies) // 2] if latencies else 0,
        latency_p95_ms=latencies[int(len(latencies) * 0.95)] if latencies else 0,
        results=results,
    )

    return summary

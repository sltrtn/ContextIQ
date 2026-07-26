"""Evaluation API routes."""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.evaluation.ragas_runner import run_evaluation, load_test_set, EvalSummary
from app.evaluation.configs import CONFIGS

router = APIRouter(prefix="/api/v1/evaluation", tags=["evaluation"])


class EvalRequest(BaseModel):
    config: str = "hybrid_rerank"
    max_questions: int | None = None


class EvalResponse(BaseModel):
    config: str
    num_questions: int
    faithfulness: float
    answer_relevancy: float
    context_precision: float
    context_recall: float
    latency_p50_ms: float
    latency_p95_ms: float
    results: list[dict]


def _summary_to_response(summary: EvalSummary) -> EvalResponse:
    return EvalResponse(
        config=summary.config,
        num_questions=summary.num_questions,
        faithfulness=round(summary.faithfulness, 4),
        answer_relevancy=round(summary.answer_relevancy, 4),
        context_precision=round(summary.context_precision, 4),
        context_recall=round(summary.context_recall, 4),
        latency_p50_ms=round(summary.latency_p50_ms, 2),
        latency_p95_ms=round(summary.latency_p95_ms, 2),
        results=[
            {
                "question_id": r.question_id,
                "question": r.question,
                "answer": r.answer[:200],
                "faithfulness": round(r.faithfulness, 4),
                "answer_relevancy": round(r.answer_relevancy, 4),
                "context_precision": round(r.context_precision, 4),
                "context_recall": round(r.context_recall, 4),
                "latency_ms": round(r.latency_ms, 2),
                "error": r.error,
            }
            for r in summary.results
        ],
    )


@router.get("/configs")
async def list_configs():
    """List available evaluation configurations."""
    return {"configs": list(CONFIGS.keys())}


@router.post("/run", response_model=EvalResponse)
async def run_eval(req: EvalRequest):
    """Run evaluation for a specific pipeline configuration."""
    if req.config not in CONFIGS:
        raise HTTPException(400, f"Unknown config '{req.config}'. Available: {list(CONFIGS.keys())}")

    query_fn = CONFIGS[req.config]

    test_set = load_test_set()
    if req.max_questions:
        test_set = test_set[: req.max_questions]

    summary = run_evaluation(
        config_name=req.config,
        query_fn=query_fn,
        test_set=test_set,
    )

    return _summary_to_response(summary)

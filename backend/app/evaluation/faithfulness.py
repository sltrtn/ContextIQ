"""Post-generation faithfulness check — lightweight LLM-as-judge for a single answer."""

import re
from app.core.llm import get_llm
from app.models.query import FaithfulnessCheck


def check_faithfulness(answer: str, contexts: list[str]) -> FaithfulnessCheck:
    """Check if the answer is grounded in the provided contexts.

    Returns a FaithfulnessCheck with score and claim details.
    """
    if not contexts or not answer.strip():
        return FaithfulnessCheck(score=0.0, supported_claims=0, total_claims=0)

    llm = get_llm()
    context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c[:2000]}" for i, c in enumerate(contexts[:5]))

    prompt = f"""You are evaluating whether an answer is faithful to the provided contexts.

CONTEXTS:
{context_block}

ANSWER:
{answer}

Task: List each distinct claim in the ANSWER. For each claim, state whether it is SUPPORTED or NOT SUPPORTED by the CONTEXTS.

Format your response EXACTLY as:
TOTAL: [number]
SUPPORTED: [number]
UNSUPPORTED:
- claim 1
- claim 2"""

    try:
        response = str(llm.complete(prompt)).strip()

        # Parse total claims
        total_match = re.search(r'TOTAL:\s*(\d+)', response)
        supported_match = re.search(r'SUPPORTED:\s*(\d+)', response)

        total = int(total_match.group(1)) if total_match else 0
        supported = int(supported_match.group(1)) if supported_match else 0

        # Parse unsupported claims
        unsupported = []
        in_unsupported = False
        for line in response.split('\n'):
            if 'UNSUPPORTED:' in line:
                in_unsupported = True
                # Check if there's a claim on the same line
                parts = line.split('UNSUPPORTED:', 1)
                if len(parts) > 1 and parts[1].strip():
                    unsupported.append(parts[1].strip().lstrip('-').strip())
                continue
            if in_unsupported:
                stripped = line.strip()
                if stripped.startswith('-'):
                    unsupported.append(stripped.lstrip('-').strip())
                elif stripped and not stripped.startswith(('TOTAL', 'SUPPORTED')):
                    break

        score = supported / total if total > 0 else 1.0

        return FaithfulnessCheck(
            score=round(score, 4),
            supported_claims=supported,
            total_claims=total,
            unsupported_claims=unsupported[:5],  # Cap at 5
        )
    except Exception as e:
        print(f"Faithfulness check failed: {e}")
        return FaithfulnessCheck(score=0.0, supported_claims=0, total_claims=0)

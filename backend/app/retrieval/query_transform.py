"""Query transformation — rewriting and expansion for better retrieval."""

import re
from app.core.llm import get_llm


def expand_query(query: str, num_variants: int = 2) -> list[str]:
    """Expand a user query into multiple phrasings for better recall.

    Returns the original query plus num_variants rephrased versions.
    """
    try:
        llm = get_llm()

        prompt = f"""Rewrite the following question in {num_variants} different ways to improve search retrieval.
Each rephrasing should use different vocabulary and phrasing but preserve the same meaning.
Focus on: technical synonyms, alternative terminology, and more specific phrasings.

Original question: {query}

Format your response as:
[1] rephrased question 1
[2] rephrased question 2

Do NOT include the original question."""

        response = str(llm.complete(prompt)).strip()
        variants = []
        for line in response.split('\n'):
            line = line.strip()
            match = re.match(r'\[(\d+)\]\s*(.*)', line)
            if match:
                text = match.group(2).strip()
                if text and text != query:
                    variants.append(text)
        # Always include the original
        return [query] + variants[:num_variants]
    except Exception as e:
        print(f"Query expansion failed: {e}")
        return [query]


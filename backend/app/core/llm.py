"""LLM factory — returns the right LLM based on config.

Supports:
  - "groq":   Groq hosted openai/gpt-oss-120b (free tier) — no billing needed
  - "openai": OpenAI GPT-4o-mini — requires billing
"""

from app.core.config import get_settings

settings = get_settings()


def get_llm():
    """Return a LlamaIndex-compatible LLM per config."""
    provider = settings.llm_provider

    if provider == "groq":
        from llama_index.llms.groq import Groq
        return Groq(
            model=settings.groq_model,
            api_key=settings.groq_api_key,
        )

    elif provider == "openai":
        from llama_index.llms.openai import OpenAI
        return OpenAI(model=settings.openai_model)

    else:
        raise ValueError(
            f"Unknown llm_provider '{provider}'. "
            "Set to 'groq' or 'openai' in .env"
        )

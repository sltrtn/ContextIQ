"""Unit tests for query expansion."""

from app.retrieval.query_transform import expand_query


class FakeLLM:
    def __init__(self, response):
        self.response = response

    def complete(self, prompt):
        return self.response


def test_expand_query_returns_variants(monkeypatch):
    fake = FakeLLM("[1] What is Quantized LoRA?\n[2] Explain QLoRA method.")
    monkeypatch.setattr("app.retrieval.query_transform.get_llm", lambda: fake)
    result = expand_query("What is QLoRA?", num_variants=2)
    assert len(result) == 3
    assert result[0] == "What is QLoRA?"
    assert result[1] == "What is Quantized LoRA?"
    assert result[2] == "Explain QLoRA method."


def test_expand_query_dedupes_original(monkeypatch):
    fake = FakeLLM("[1] What is QLoRA?\n[2] Explain QLoRA.")
    monkeypatch.setattr("app.retrieval.query_transform.get_llm", lambda: fake)
    result = expand_query("What is QLoRA?", num_variants=2)
    # Original is included once, duplicate variant is dropped, only one new variant kept
    assert result[0] == "What is QLoRA?"
    assert "Explain QLoRA." in result
    assert len(result) == 2


def test_expand_query_fallback_on_error(monkeypatch):
    def bad_llm():
        raise Exception("no key")
    monkeypatch.setattr("app.retrieval.query_transform.get_llm", bad_llm)
    result = expand_query("What is QLoRA?", num_variants=2)
    assert result == ["What is QLoRA?"]


def test_expand_query_ignores_malformed_lines(monkeypatch):
    fake = FakeLLM("Here is a variant: [1] Variant one")
    monkeypatch.setattr("app.retrieval.query_transform.get_llm", lambda: fake)
    result = expand_query("What is QLoRA?", num_variants=2)
    # Original + one parsed variant
    assert result[0] == "What is QLoRA?"
    assert len(result) >= 1

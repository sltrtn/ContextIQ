#!/usr/bin/env python3
"""ContextIQ flashcard drill.

Run with:
    python Ref/interview/flashcard_quiz.py

Rules:
- Read the question out loud.
- Say your answer.
- Press Enter to reveal.
- Grade yourself: y = correct, n = incorrect, s = skip.
- Missed cards go into a review pile until you get them right.
"""

import random
import sys

FLASHCARDS = [
    ("What is RAG?", "Retrieval-Augmented Generation: retrieve relevant chunks, augment the LLM prompt with them, then generate an answer."),
    ("Why not just paste the whole document into the LLM?", "Context window limits, cost, and worse accuracy due to noise / lost-in-the-middle effect."),
    ("What is an embedding?", "Text converted into a vector where similar meanings are close together."),
    ("What is a vector database?", "A database optimized to find stored vectors close to a query vector."),
    ("What is the difference between dense and sparse retrieval?", "Dense searches by semantic meaning (embeddings). Sparse searches by exact keyword match (BM25)."),
    ("Why is fine-tuning not the right solution here?", "Expensive, requires retraining when documents change, and cannot cite sources."),
    ("What web framework does ContextIQ use?", "FastAPI."),
    ("What is the default embedding provider and model?", "fastembed with BAAI/bge-small-en-v1.5, producing 384-dimensional vectors."),
    ("What is the default LLM provider and model?", "Groq with llama-3.3-70b-versatile."),
    ("Why are fastembed and Groq the defaults?", "OpenAI key had no billing credits; fastembed and Groq are free for development."),
    ("What pattern do embeddings.py and llm.py use?", "Factory / strategy pattern — provider is swappable."),
    ("What file types can be uploaded?", "PDF, DOCX, TXT."),
    ("What library parses PDFs?", "pypdf."),
    ("Why preserve page numbers during parsing?", "So the final answer can cite specific pages like (p.5)."),
    ("When is the BM25 index rebuilt?", "After every document upload, using all accumulated chunks."),
    ("What are the three chunking strategies?", "Sentence-window, semantic, contextual."),
    ("Which chunking strategy is the default?", "Contextual chunking."),
    ("How does contextual chunking work?", "Detect sections, summarize each with one batched LLM call, prepend [Section: Name — Summary] to chunks."),
    ("What vector database is used?", "Qdrant."),
    ("What distance metric does Qdrant use?", "Cosine."),
    ("What is the difference between a bi-encoder and a cross-encoder?", "Bi-encoder encodes query and document independently (fast). Cross-encoder encodes them together (more accurate, slower)."),
    ("What is BM25?", "A keyword-based ranking function that scores documents by term frequency and document length."),
    ("Why is BM25 a global singleton built at ingest time?", "An earlier per-query rebuild from dense results only searched top-20 dense chunks, not the full corpus."),
    ("What is RRF and what is the constant k?", "Reciprocal Rank Fusion merges ranked lists by summing 1/(rank + k). k=60."),
    ("Why does RRF use rank instead of raw score?", "Dense cosine similarity and BM25 scores are on different scales. Ranks are comparable."),
    ("What reranker model is used?", "Cohere rerank-english-v3.0."),
    ("Why is there a 6.1-second delay between rerank calls?", "Cohere trial tier limits to 10 calls per minute."),
    ("What happens if the Cohere rerank call fails?", "Falls back to returning the top-k input documents in original order."),
    ("What are the three jobs of context assembly?", "Deduplicate, order, label."),
    ("Why is the most relevant chunk placed last in the context string?", "LLMs attend better to text near the question at the end of the context."),
    ("What does a source label look like?", "[1] filename.pdf (p.5): chunk text..."),
    ("What is the default query config?", "hybrid_rerank."),
    ("List the five configs.", "vector_only, vector_rerank, hybrid, hybrid_rerank, long_context."),
    ("Which config performed best on the benchmark?", "vector_rerank (P@5 = 0.9933, MRR = 1.0)."),
    ("Which config performed worst?", "long_context (P@5 = 0.20)."),
    ("What does query expansion do?", "Rewrites the question into variants to improve recall, then fuses results."),
    ("When does the faithfulness check run?", "After the answer is generated."),
    ("What does P@5 measure?", "Of the top-5 retrieved chunks, how many are relevant."),
    ("What does R@5 measure?", "Of all relevant chunks in the corpus, how many appear in the top-5."),
    ("What does MRR measure?", "Average reciprocal rank of the first relevant chunk across questions."),
    ("Why is recall low (~10%) despite high precision?", "Each paper has 20–173 relevant chunks; taking only the top-5 caps recall mechanically."),
    ("What is the headline insight from the benchmark?", "hybrid_rerank underperforms vector_rerank and hybrid — stacking techniques must be validated."),
    ("What is a limitation of the faithfulness check?", "It uses the same model family as the generator, so it can share blind spots."),
    ("What is the honest answer to 'Is this in production?'", "No. Qdrant is in-memory, BM25 is lost on restart, and auth/rate limiting are not wired yet."),
    ("What is the correct statement about RAGAs?", "We do not use the RAGAs library. We built a custom LLM-as-judge because RAGAs had a broken import."),
    ("How many tests are there?", "39."),
    ("Say the one-line pitch.", "Most RAG demos are judged by vibes — 'it gave a good answer.' I built ContextIQ to treat retrieval quality as a measurable engineering property: five retrieval configurations benchmarked on a 30-question test set across precision, recall, and MRR, with a faithfulness check on every live query."),
]


def main():
    print("=" * 60)
    print("ContextIQ Interview Flashcards")
    print("=" * 60)
    print("Read each question, say the answer, press Enter to reveal.")
    print("Grade yourself: y = correct, n = incorrect, s = skip, q = quit\n")

    deck = list(FLASHCARDS)
    random.shuffle(deck)
    correct = 0
    incorrect = 0
    missed = []

    while deck:
        question, answer = deck.pop(0)
        print(f"\nQ: {question}")
        try:
            input("[Press Enter to reveal...]")
        except (EOFError, KeyboardInterrupt):
            print("\nQuitting.")
            sys.exit(0)

        print(f"A: {answer}")
        while True:
            grade = input("Did you get it? (y/n/s/q): ").strip().lower()
            if grade in ("y", "n", "s", "q"):
                break
            print("Please enter y, n, s, or q.")

        if grade == "q":
            break
        elif grade == "y":
            correct += 1
        elif grade == "n":
            incorrect += 1
            missed.append((question, answer))
        elif grade == "s":
            missed.append((question, answer))

    # Review missed cards
    if missed:
        print(f"\n\nReview round: {len(missed)} card(s) to retry.")
        random.shuffle(missed)
        deck = missed
        while deck:
            question, answer = deck.pop(0)
            print(f"\nQ: {question}")
            try:
                input("[Press Enter to reveal...]")
            except (EOFError, KeyboardInterrupt):
                print("\nQuitting.")
                sys.exit(0)
            print(f"A: {answer}")
            while True:
                grade = input("Correct now? (y/n/q): ").strip().lower()
                if grade in ("y", "n", "q"):
                    break
                print("Please enter y, n, or q.")
            if grade == "q":
                break
            if grade == "n":
                deck.append((question, answer))
                random.shuffle(deck)

    total = correct + incorrect
    if total > 0:
        pct = correct / total * 100
        print(f"\n\nSession complete. Correct: {correct}, Incorrect: {incorrect}, Accuracy: {pct:.1f}%")
    else:
        print("\n\nNo cards graded.")


if __name__ == "__main__":
    main()

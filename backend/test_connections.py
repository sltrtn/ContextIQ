"""Verify all external service connections.

Usage:
    python test_connections.py

Expected output:
    OpenAI: <model-id>
    Qdrant: <collections list> or "in-memory OK"
    Cohere: <first 2 tokens of 'test'>
"""

from openai import OpenAI
from qdrant_client import QdrantClient
import cohere
import sys

from app.core.config import get_settings

settings = get_settings()

errors = []

print("=" * 50)
print("ContextIQ — Connection Test")
print("=" * 50)

# --- OpenAI ---
print("\n[1/3] OpenAI ... ", end="", flush=True)
try:
    client = OpenAI(api_key=settings.openai_api_key)
    models = client.models.list()
    print(f"OK — {models.data[0].id}")
except Exception as e:
    print(f"FAIL — {e}")
    errors.append(f"OpenAI: {e}")

# --- Qdrant ---
print("[2/3] Qdrant ... ", end="", flush=True)
try:
    if settings.qdrant_url == ":memory:":
        qdrant = QdrantClient(location=":memory:")
    else:
        kwargs = {"url": settings.qdrant_url}
        if settings.qdrant_api_key:
            kwargs["api_key"] = settings.qdrant_api_key
        qdrant = QdrantClient(**kwargs)
    collections = qdrant.get_collections()
    print(f"OK — {len(collections.collections)} collection(s)")
except Exception as e:
    print(f"FAIL — {e}")
    errors.append(f"Qdrant: {e}")

# --- Cohere ---
print("[3/3] Cohere ... ", end="", flush=True)
try:
    co = cohere.Client(settings.cohere_api_key)
    response = co.chat(model="command-r-08-2024", message="Hello")
    print(f"OK — {response.text[:50]}...")
except Exception as e:
    print(f"FAIL — {e}")
    errors.append(f"Cohere: {e}")

# --- Summary ---
print("\n" + "=" * 50)
if errors:
    print(f"FAILED — {len(errors)} connection(s) failed:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
else:
    print("All connections OK — Day 0 ready.")
    sys.exit(0)

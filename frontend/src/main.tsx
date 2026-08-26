import { useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

type Pipeline = "vector_rerank" | "hybrid" | "hybrid_rerank";
type ApiSource = { text: string; score: number; filename?: string | null; page?: number | null };
type QueryResponse = { answer: string; sources: ApiSource[]; metadata: { model?: string; latency_ms?: number; num_sources?: number }; faithfulness?: { score: number } | null };

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
const pipelineCopy: Record<Pipeline, { label: string; detail: string }> = {
  vector_rerank: { label: "VECTOR + RERANK", detail: "P@5 0.9933 · benchmark winner" },
  hybrid: { label: "HYBRID / RRF", detail: "MRR 1.0000 · speed / quality" },
  hybrid_rerank: { label: "FULL PIPELINE", detail: "Dense + BM25 + RRF + rerank" },
};

async function request(path: string, init?: RequestInit) {
  const response = await fetch(`${API_BASE}${path}`, init);
  if (response.ok) return response.json();
  let message = `Request failed (${response.status})`;
  try { message = (await response.json()).detail ?? message; } catch { /* Non-JSON error response. */ }
  throw new Error(message);
}

function App() {
  const [pipeline, setPipeline] = useState<Pipeline>("vector_rerank");
  const [question, setQuestion] = useState("What does DPO stand for and what problem does it solve?");
  const [fileName, setFileName] = useState("NO DOCUMENT LOADED");
  const [hasDocument, setHasDocument] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [querying, setQuerying] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<QueryResponse | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const upload = async (file?: File) => {
    if (!file || uploading) return;
    setError(""); setResult(null); setHasDocument(false); setUploading(true); setFileName(`UPLOADING / ${file.name}`);
    try {
      const form = new FormData(); form.append("file", file);
      await request("/documents/upload", { method: "POST", body: form });
      setFileName(file.name); setHasDocument(true);
    } catch (caught) {
      setFileName("UPLOAD FAILED"); setError(caught instanceof Error ? caught.message : "Upload failed.");
    } finally {
      setUploading(false); if (inputRef.current) inputRef.current.value = "";
    }
  };

  const ask = async () => {
    if (!question.trim() || uploading || querying || !hasDocument) return;
    setError(""); setResult(null); setQuerying(true);
    try {
      const data = await request("/query", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ question, top_k: 5, config: pipeline }) });
      setResult(data as QueryResponse);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Query failed.");
    } finally { setQuerying(false); }
  };

  return <main className="shell">
    <header className="topbar"><a className="wordmark" href="#top" aria-label="ContextIQ home">CONTEXT<span>IQ</span></a><p className="topline">RESEARCH, INTERROGATED.</p><button className="menu-button magnetic" type="button" aria-label="Open navigation"><i /><i /></button></header>
    <section className="hero" id="top"><div className="eyebrow"><span />RAG / RESEARCH INTELLIGENCE / 001</div><h1>READ BETWEEN<br />THE <em>LINES.</em></h1><p className="hero-note">Upload a paper. Interrogate the evidence. Follow every answer to its source.</p><div className="marquee" aria-hidden="true"><div>QUESTION EVERYTHING — QUESTION EVERYTHING — QUESTION EVERYTHING — </div></div></section>
    <section className="workbench" aria-label="ContextIQ query workspace">
      <div className="panel upload-panel"><p className="panel-index">01 / SOURCE</p><input ref={inputRef} className="visually-hidden" type="file" accept=".pdf,.docx,.txt" onChange={(event) => void upload(event.target.files?.[0])} />
        <button className={`dropzone ${isDragging ? "is-dragging" : ""}`} type="button" disabled={uploading} onClick={() => inputRef.current?.click()} onDragOver={(event) => { event.preventDefault(); setIsDragging(true); }} onDragLeave={() => setIsDragging(false)} onDrop={(event) => { event.preventDefault(); setIsDragging(false); void upload(event.dataTransfer.files[0]); }}>
          <strong>{uploading ? <>INDEXING<br />EVIDENCE</> : <>DROP THE<br />EVIDENCE <b>+</b></>}</strong><span>{uploading ? "PARSING / CHUNKING / INDEXING" : "PDF / DOCX / TXT"}</span>
        </button><div className="file-strip"><span>{uploading ? "WORKING" : "LOADED"}</span><b>{fileName}</b><button type="button" onClick={() => inputRef.current?.click()} aria-label="Replace document">↗</button></div>
      </div>
      <div className="panel query-panel"><p className="panel-index">02 / INTERROGATE</p><label htmlFor="question">ASK THE DOCUMENT</label><textarea id="question" value={question} onChange={(event) => setQuestion(event.target.value)} onKeyDown={(event) => { if ((event.metaKey || event.ctrlKey) && event.key === "Enter") void ask(); }} />
        <div className="query-footer"><span>{querying ? "RETRIEVING EVIDENCE…" : "⌘ + ENTER TO RUN"}</span><button className="ask-button" type="button" disabled={uploading || querying || !hasDocument} onClick={() => void ask()}><span>{querying ? "THINKING" : "ASK"}</span><b>{querying ? "…" : "→"}</b></button></div>
      </div>
      <aside className="panel controls-panel"><p className="panel-index">03 / METHOD</p><div className="pipeline-list">{(Object.keys(pipelineCopy) as Pipeline[]).map((item, index) => <button className={`pipeline ${pipeline === item ? "is-active" : ""}`} key={item} type="button" onClick={() => setPipeline(item)}><span>0{index + 1}</span><strong>{pipelineCopy[item].label}</strong><i>↗</i></button>)}</div><div className="method-detail"><b>{pipelineCopy[pipeline].detail}</b><span>MEASURED, NOT ASSUMED.</span></div></aside>
    </section>
    {error && <p className="error-banner" role="alert">API ERROR / {error}</p>}
    <section className={`answer-section ${result ? "is-visible" : ""}`} aria-live="polite">
      <div className="answer-head"><p className="panel-index">04 / RESPONSE</p><p>{result?.metadata.model?.toUpperCase() ?? "WAITING FOR QUERY"}</p></div>
      {result && <><div className="answer-grid"><div className="answer-main"><p className="answer-number">A.</p><h2>{result.answer}</h2><button className="citation-button" type="button" onClick={() => document.querySelector(".source-grid")?.scrollIntoView({ behavior: "smooth" })}>VIEW EVIDENCE ↗</button></div><div className="answer-meta"><div><span>FAITHFULNESS</span><b>{result.faithfulness ? `${result.faithfulness.score.toFixed(2)} / 1.00` : "NOT SCORED"}</b></div><div><span>RETRIEVED</span><b>{result.metadata.num_sources ?? result.sources.length} SOURCES</b></div><div><span>LATENCY</span><b>{result.metadata.latency_ms ? `${(result.metadata.latency_ms / 1000).toFixed(2)} SEC` : "—"}</b></div></div></div><div className="source-grid">{result.sources.map((source, index) => <button className="source-card" key={`${source.filename}-${index}`} type="button" title={source.text}><span>[{String(index + 1).padStart(2, "0")}]</span><strong>{source.filename ?? "UNTITLED SOURCE"}</strong><b>{source.page ? `P. ${source.page}` : "PAGE N/A"} ↗</b></button>)}</div></>}
    </section>
    <footer><p>CONTEXTIQ / BUILT TO BE MEASURED.</p><p>© 2026</p></footer>
  </main>;
}

createRoot(document.getElementById("root")!).render(<App />);

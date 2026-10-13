function highlight(text, query) {
  if (!query) return text;
  const lower = text.toLowerCase();
  const q = query.toLowerCase();
  const parts = [];
  let i = 0;
  while (i < text.length) {
    const idx = lower.indexOf(q, i);
    if (idx === -1) {
      parts.push(text.slice(i));
      break;
    }
    if (idx > i) parts.push(text.slice(i, idx));
    parts.push(<mark key={idx}>{text.slice(idx, idx + q.length)}</mark>);
    i = idx + q.length;
  }
  return parts;
}

export default function SearchResults({ query, result, isSearching }) {
  if (isSearching) {
    return <p className="result-summary" role="status">Searching…</p>;
  }

  if (!result) {
    return (
      <p className="result-summary">
        Build an index, then search a keyword to see matching pages.
      </p>
    );
  }

  if (result.resultCount === 0) {
    return (
      <p className="result-summary" role="status">
        No matches for “{result.query}”.
      </p>
    );
  }

  return (
    <div className="search-results">
      <p className="result-summary" role="status">
        {result.resultCount} match{result.resultCount === 1 ? "" : "es"} across{" "}
        {result.groups.length} page{result.groups.length === 1 ? "" : "s"}
      </p>

      {result.groups.map((group) => (
        <section className="result-group" key={group.url}>
          <a className="result-url" href={group.url} target="_blank" rel="noreferrer">
            {group.url}
          </a>
          <ul className="result-contexts">
            {group.contexts.map((line, index) => (
              <li key={index}>{highlight(line, query)}</li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

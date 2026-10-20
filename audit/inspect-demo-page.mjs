const pages = await fetch('http://127.0.0.1:9333/json/list').then(r => r.json());
const ws = new WebSocket(pages.find(p => p.type === 'page').webSocketDebuggerUrl);
const expression = process.argv[2] || `JSON.stringify({path:location.pathname,alerts:[...document.querySelectorAll('[role=alert]')].map(e=>e.textContent),titles:[...document.querySelectorAll('h3')].map(e=>e.textContent),status:[...document.querySelectorAll('[role=status]')].map(e=>e.textContent)})`;
ws.onopen = () => ws.send(JSON.stringify({ id: 1, method: 'Runtime.evaluate', params: { expression, returnByValue: true, awaitPromise: true } }));
ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id === 1) { console.log(m.result?.result?.value || m.result?.exceptionDetails); ws.close(); } };

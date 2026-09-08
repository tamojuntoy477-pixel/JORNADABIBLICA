const limits = globalThis.__noxLimits || (globalThis.__noxLimits = new Map());

function systemFor(mode) {
  const base = 'Você é NOX ULTRA AI, uma assistente útil, clara, criativa e segura. Responda no idioma do usuário. Não finja capacidades que não possui.';
  if (mode === 'code') return base + ' Priorize código correto, explique bugs de forma prática e use blocos de código quando necessário.';
  if (mode === 'research') return base + ' Priorize fatos verificáveis e pesquisa atual. Diferencie fatos de incertezas.';
  if (mode === 'creative') return base + ' Priorize criatividade, ideias originais e boa apresentação.';
  if (mode === 'ultra') return base + ' Trabalhe com alto cuidado: verifique a própria resposta e priorize precisão.';
  if (mode === 'ultra-review') return base + ' Você é o revisor final. Elimine erros, contradições e conteúdo inútil.';
  return base + ' Escolha a abordagem mais adequada para a tarefa.';
}

function extractText(json) {
  if (typeof json.output_text === 'string' && json.output_text.trim()) return json.output_text.trim();
  const parts = [];
  for (const item of json.output || []) {
    for (const part of item.content || []) {
      if (part.type === 'output_text' && part.text) parts.push(part.text);
    }
  }
  return parts.join('\n').trim();
}

async function callOpenAI({ model, instructions, input, web, effort }) {
  const body = { model, instructions, input, max_output_tokens: 2500 };
  if (effort) body.reasoning = { effort };
  if (web) body.tools = [{ type: 'web_search' }];

  const r = await fetch('https://api.openai.com/v1/responses', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(body)
  });
  const json = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(json?.error?.message || `Erro da API (${r.status})`);
  const text = extractText(json);
  if (!text) throw new Error('A IA respondeu sem texto.');
  return text;
}

export default async function handler(req, res) {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'POST') return res.status(405).json({ error: 'Método não permitido.' });
  if (!process.env.OPENAI_API_KEY) return res.status(503).json({ error: 'Servidor da NOX ainda não foi ativado.' });

  const ip = (req.headers['x-forwarded-for'] || req.socket?.remoteAddress || 'unknown').toString().split(',')[0].trim();
  const now = Date.now();
  const entry = limits.get(ip) || { start: now, count: 0 };
  if (now - entry.start > 10 * 60 * 1000) { entry.start = now; entry.count = 0; }
  entry.count++;
  limits.set(ip, entry);
  if (entry.count > 30) return res.status(429).json({ error: 'Muitas mensagens em pouco tempo. Tente novamente mais tarde.' });

  const prompt = typeof req.body?.prompt === 'string' ? req.body.prompt.trim() : '';
  const modeRaw = typeof req.body?.mode === 'string' ? req.body.mode : 'auto';
  const allowed = new Set(['auto','ultra','code','research','creative']);
  const mode = allowed.has(modeRaw) ? modeRaw : 'auto';
  if (!prompt) return res.status(400).json({ error: 'Escreva uma mensagem.' });
  if (prompt.length > 12000) return res.status(400).json({ error: 'Mensagem muito longa.' });

  try {
    const high = mode === 'ultra' || mode === 'code' || mode === 'research';
    const model = high ? 'gpt-5.6-sol' : 'gpt-5.6-terra';
    let text = await callOpenAI({
      model,
      instructions: systemFor(mode),
      input: prompt,
      web: mode === 'research',
      effort: high ? 'high' : 'medium'
    });

    if (mode === 'ultra') {
      text = await callOpenAI({
        model: 'gpt-5.6-sol',
        instructions: systemFor('ultra-review'),
        input: 'Revise a resposta abaixo. Corrija erros, melhore clareza e precisão e devolva apenas a resposta final melhorada.\n\nRESPOSTA:\n' + text,
        web: false,
        effort: 'high'
      });
    }
    return res.status(200).json({ text });
  } catch (e) {
    return res.status(500).json({ error: e?.message || 'Falha ao consultar a IA.' });
  }
}

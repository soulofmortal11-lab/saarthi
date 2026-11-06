// SAARTHI's cloud brain: a Cloudflare Worker.
// It keeps the Claude API key off the phone, checks the access code,
// gives Saarthi its personality, and asks Claude for a reply.
//
// Secrets to set in Cloudflare (Settings > Variables and Secrets):
//   ANTHROPIC_API_KEY    your Claude API key
//   SAARTHI_ACCESS_CODE  any phrase you choose; the app must send the same one
// Optional plain variable:
//   SAARTHI_MODEL        defaults to claude-haiku-5-5

const DEFAULT_MODEL = "claude-haiku-5-5";
const MAX_TURNS = 20;
const MAX_CHARS_PER_TURN = 2000;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/") {
      return json({ name: "Saarthi brain", ok: true });
    }

    const code = request.headers.get("X-Saarthi-Code") || "";
    if (!env.SAARTHI_ACCESS_CODE || code !== env.SAARTHI_ACCESS_CODE) {
      return json({ error: "The access code doesn't match the server." }, 401);
    }
    if (!env.ANTHROPIC_API_KEY) {
      return json({ error: "The server has no ANTHROPIC_API_KEY secret yet." }, 500);
    }

    if (url.pathname === "/health") {
      return json({ ok: true, model: env.SAARTHI_MODEL || DEFAULT_MODEL });
    }

    if (url.pathname === "/chat" && request.method === "POST") {
      return chat(request, env);
    }

    return json({ error: "Not found." }, 404);
  },
};

async function chat(request, env) {
  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "The request wasn't valid JSON." }, 400);
  }

  const messages = cleanMessages(body.messages);
  if (messages.length === 0) {
    return json({ error: "There was nothing to reply to." }, 400);
  }

  const mode = body.mode === "night" ? "night" : "day";
  const localTime = typeof body.localTime === "string" ? body.localTime.slice(0, 80) : "unknown";

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": "2026-10-21",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: env.SAARTHI_MODEL || DEFAULT_MODEL,
      max_tokens: 800,
      output_config: { effort: "low" },
      system: systemPrompt(mode, localTime),
      messages,
    }),
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    return json({ error: explainApiError(response.status, data) }, 502);
  }

  const reply = (data.content || [])
    .filter((block) => block.type === "text")
    .map((block) => block.text)
    .join("")
    .trim();

  if (!reply) {
    return json({ error: "Claude sent back an empty reply." }, 502);
  }

  return json({ reply });
}

// Keep the last turns, trim long ones, start with the user, and alternate roles.
function cleanMessages(raw) {
  if (!Array.isArray(raw)) return [];
  const result = [];
  for (const item of raw.slice(-MAX_TURNS)) {
    if (!item || (item.role !== "user" && item.role !== "assistant")) continue;
    if (typeof item.content !== "string") continue;
    const content = item.content.trim().slice(0, MAX_CHARS_PER_TURN);
    if (!content) continue;
    const last = result[result.length - 1];
    if (last && last.role === item.role) {
      last.content += "\n" + content;
    } else {
      result.push({ role: item.role, content });
    }
  }
  while (result.length && result[0].role !== "user") result.shift();
  if (result.length && result[result.length - 1].role !== "user") return [];
  return result;
}

function explainApiError(status, data) {
  const message = (data && data.error && data.error.message) || "";
  if (status === 401) return "The Claude API key on the server isn't valid.";
  if (/credit balance/i.test(message)) return "The Claude API account is out of credit.";
  if (status === 429) return "Too many requests right now. Try again in a moment.";
  if (status === 529 || /overloaded/i.test(message)) return "Claude is busy right now. Try again in a moment.";
  return `Claude returned an error (${status}). ${message}`.trim();
}

function systemPrompt(mode, localTime) {
  return `You are SAARTHI (सारथी), a personal voice assistant that lives on the user's Android phone. Your name means charioteer, the guide who drives the chariot, as Krishna guided Arjuna. It also stands for Smart Adaptive Assistant for Real-Time Help and Intelligence.

Personality: a witty, polished butler with a warm, friendly side. Sharp and elegant, with a light touch of dry humour; relaxed and playful in casual chat. You may occasionally call the user "Sir", but don't overdo it. Confident, never servile, never preachy.

Everything you write is spoken aloud by text-to-speech, so:
- Keep replies short: usually one to three sentences, unless the user asks for detail.
- No markdown, lists, bullet points, headings, emojis, links or code.
- Write numbers, times and symbols the way they should be said aloud.

Language:
- If the user speaks English, reply in English.
- If the user speaks Hindi or Hinglish, reply in Hindi written in Devanagari script. Never write Hindi in Roman letters. Common English words may stay in English.

What you can do right now: talk, answer questions, explain things, brainstorm and keep the user company. You cannot yet make calls, send messages, set alarms or reminders, open apps, read notifications or see the screen. Those abilities are coming in future versions. If asked, say so briefly and charmingly, and never pretend you did something you can't do.

You have no live internet access. For news, weather, prices or live scores, say you can't check live information yet.

Speech recognition sometimes mishears words. If a request seems garbled, make a sensible guess, or ask a short question.

Context: the user's local time is ${localTime}. The app is in ${mode} mode.`;
}

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

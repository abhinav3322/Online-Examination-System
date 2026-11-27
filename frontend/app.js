const state = {
  sessionId: null,
  examId: null,
  timerInterval: null,
  secondsLeft: 0
};

// Tab navigation
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(btn.dataset.tab).classList.add('active');

    if (btn.dataset.tab === 'add-question') loadQuestions();
    if (btn.dataset.tab === 'create-exam') loadQuestionsForChecklist();
    if (btn.dataset.tab === 'history-toppers') {
      loadToppers();
      loadHistory();
    }
  });
});

function showAlert(msg, isError = false) {
  const box = document.getElementById('alert-box');
  box.textContent = msg;
  box.className = isError ? 'alert error' : 'alert';
  setTimeout(() => box.classList.add('hidden'), 4000);
}

async function loadExams() {
  const res = await fetch('/api/exams');
  const exams = await res.json();
  const select = document.getElementById('exam-select');
  const topperFilter = document.getElementById('topper-exam-filter');

  select.innerHTML = exams.map(e =>
    `<option value="${e.id}">${e.title} (${e.questionCount} Qs, ${e.durationMinutes} min)</option>`
  ).join('');

  topperFilter.innerHTML = `<option value="0">All Exams</option>` + exams.map(e =>
    `<option value="${e.id}">${e.title}</option>`
  ).join('');
}

async function loadQuestions() {
  const res = await fetch('/api/questions');
  const questions = await res.json();
  document.getElementById('question-table-body').innerHTML = questions.map(q =>
    `<tr><td>${q.id}</td><td>${q.subject}</td><td>${q.prompt}</td><td><strong>${q.correctOption}</strong></td></tr>`
  ).join('');
}

async function loadQuestionsForChecklist() {
  const res = await fetch('/api/questions');
  const questions = await res.json();
  document.getElementById('exam-question-checklist').innerHTML = questions.map(q =>
    `<label class="option-label">
      <input type="checkbox" name="exam-q" value="${q.id}">
      <span><strong>[${q.subject}]</strong> ${q.prompt}</span>
    </label>`
  ).join('');
}

// Add Question
document.getElementById('question-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    subject: document.getElementById('q-subject').value,
    prompt: document.getElementById('q-prompt').value,
    optA: document.getElementById('q-opt-a').value,
    optB: document.getElementById('q-opt-b').value,
    optC: document.getElementById('q-opt-c').value,
    optD: document.getElementById('q-opt-d').value,
    correctOption: document.getElementById('q-correct').value
  };
  const res = await fetch('/api/questions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (data.success) {
    showAlert('Question saved successfully!');
    e.target.reset();
    loadQuestions();
  } else {
    showAlert(data.message || 'Failed to save question', true);
  }
});

// Create Exam
document.getElementById('exam-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const checked = Array.from(document.querySelectorAll('input[name="exam-q"]:checked')).map(cb => cb.value);
  const payload = {
    title: document.getElementById('e-title').value,
    durationMinutes: document.getElementById('e-duration').value,
    questionIds: checked.join(',')
  };
  const res = await fetch('/api/exams', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (data.success) {
    showAlert('Exam created!');
    e.target.reset();
    loadExams();
  } else {
    showAlert(data.message || 'Failed to create exam', true);
  }
});

// Start Timed Exam
document.getElementById('start-exam-btn').addEventListener('click', async () => {
  const name = document.getElementById('student-name').value.trim();
  const roll = document.getElementById('student-roll').value.trim();
  const examId = document.getElementById('exam-select').value;
  if (!name || !roll || !examId) {
    showAlert('Please enter your name, roll number, and select an exam.', true);
    return;
  }

  const res = await fetch('/api/attempt/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ examId })
  });
  const data = await res.json();
  if (!data.success) {
    showAlert(data.message || 'Could not start exam', true);
    return;
  }

  state.sessionId = data.sessionId;
  state.examId = examId;
  state.secondsLeft = data.durationSeconds;

  document.getElementById('active-exam-title').textContent = data.examTitle;
  document.getElementById('active-student-label').textContent = `${name} (${roll})`;

  const letters = ['A', 'B', 'C', 'D'];
  document.getElementById('questions-container').innerHTML = data.questions.map((q, idx) =>
    `<div class="question-box" data-qid="${q.id}">
      <div><strong>Q${idx + 1}.</strong> ${q.prompt}</div>
      ${q.options.map((opt, i) => `
        <label class="option-label">
          <input type="radio" name="q_${q.id}" value="${letters[i]}">
          <span><strong>${letters[i]}.</strong> ${opt}</span>
        </label>
      `).join('')}
    </div>`
  ).join('');

  document.getElementById('exam-setup').classList.add('hidden');
  document.getElementById('exam-result').classList.add('hidden');
  document.getElementById('exam-runner').classList.remove('hidden');

  updateTimerDisplay();
  clearInterval(state.timerInterval);
  state.timerInterval = setInterval(() => {
    state.secondsLeft--;
    updateTimerDisplay();
    if (state.secondsLeft <= 0) {
      clearInterval(state.timerInterval);
      submitExam(true);
    }
  }, 1000);
});

function updateTimerDisplay() {
  const mins = String(Math.floor(Math.max(0, state.secondsLeft) / 60)).padStart(2, '0');
  const secs = String(Math.max(0, state.secondsLeft) % 60).padStart(2, '0');
  document.getElementById('timer-display').textContent = `${mins}:${secs}`;
}

document.getElementById('submit-exam-btn').addEventListener('click', () => submitExam(false));

async function submitExam(autoSubmit) {
  clearInterval(state.timerInterval);
  const answerPairs = [];
  document.querySelectorAll('.question-box').forEach(box => {
    const qid = box.dataset.qid;
    const chosen = box.querySelector(`input[name="q_${qid}"]:checked`);
    if (chosen) answerPairs.push(`${qid}:${chosen.value}`);
  });

  const payload = {
    sessionId: state.sessionId,
    examId: state.examId,
    studentName: document.getElementById('student-name').value.trim(),
    studentRoll: document.getElementById('student-roll').value.trim(),
    answers: answerPairs.join(','),
    autoSubmit: String(autoSubmit)
  };

  const res = await fetch('/api/attempt/submit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json();
  if (!data.success) {
    showAlert(data.message || 'Submission failed', true);
    return;
  }

  document.getElementById('exam-runner').classList.add('hidden');
  document.getElementById('exam-result').classList.remove('hidden');
  document.getElementById('res-score').textContent = `${data.score} / ${data.total}`;
  document.getElementById('res-time').textContent = `${data.timeTakenSec}s`;

  document.getElementById('result-breakdown').innerHTML = data.breakdown.map((b, i) =>
    `<div class="review-item ${b.correct ? 'correct' : 'wrong'}">
      <div><strong>Q${i + 1}.</strong> ${b.prompt}</div>
      <div>Your Answer: <strong>${b.selectedOption || 'Not Answered'}</strong> | Correct Answer: <strong>${b.correctOption}</strong></div>
    </div>`
  ).join('');
}

document.getElementById('retake-btn').addEventListener('click', () => {
  document.getElementById('exam-result').classList.add('hidden');
  document.getElementById('exam-setup').classList.remove('hidden');
});

async function loadToppers() {
  const examId = document.getElementById('topper-exam-filter').value || 0;
  const res = await fetch(`/api/toppers?examId=${examId}`);
  const rows = await res.json();
  document.getElementById('toppers-body').innerHTML = rows.map((r, i) =>
    `<tr><td>#${i + 1}</td><td>${r.studentName}</td><td>${r.studentRoll}</td><td>${r.examTitle}</td><td><strong>${r.score}/${r.total}</strong></td><td>${r.timeTakenSec}s</td></tr>`
  ).join('');
}

async function loadHistory() {
  const roll = encodeURIComponent(document.getElementById('history-roll-filter').value.trim());
  const res = await fetch(`/api/history?roll=${roll}`);
  const rows = await res.json();
  document.getElementById('history-body').innerHTML = rows.map(r =>
    `<tr><td>${r.id}</td><td>${r.studentName}</td><td>${r.studentRoll}</td><td>${r.examTitle}</td><td>${r.score}/${r.total}</td><td>${r.timeTakenSec}s</td><td>${r.submittedAt}</td></tr>`
  ).join('');
}

document.getElementById('topper-exam-filter').addEventListener('change', loadToppers);
document.getElementById('history-roll-filter').addEventListener('input', loadHistory);

// Initial load
loadExams();

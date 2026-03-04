const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
        y: { beginAtZero: true, max: 100, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#A1A1A6' } },
        x: { display: false }
    },
    plugins: { legend: { display: false } },
    elements: { line: { tension: 0.4 }, point: { radius: 0 } }
};

const cpuCtx = document.getElementById('cpuChart').getContext('2d');
const cpuChart = new Chart(cpuCtx, {
    type: 'line',
    data: {
        labels: Array(20).fill(''),
        datasets: [{
            data: Array(20).fill(0),
            borderColor: '#007AFF',
            borderWidth: 3,
            fill: true,
            backgroundColor: 'rgba(0,122,255,0.1)'
        }]
    },
    options: chartOptions
});

const memCtx = document.getElementById('memoryChart').getContext('2d');
const memChart = new Chart(memCtx, {
    type: 'line',
    data: {
        labels: Array(20).fill(''),
        datasets: [{
            data: Array(20).fill(0),
            borderColor: '#5856D6',
            borderWidth: 3,
            fill: true,
            backgroundColor: 'rgba(88,86,214,0.1)'
        }]
    },
    options: chartOptions
});

// For dashboard, we use a simple approach: since it's local and password might not be set yet,
// we try to get a temporary token or use the one from config if possible.
// However, the cleanest way is for the user to enter it.
// For now, let's assume the dashboard route is protected or the user provides token in URL.
let authToken = new URLSearchParams(window.location.search).get('token') || 'debug_token_123';

async function updateData() {
    try {
        const response = await fetch('/api/system', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (!response.ok) throw new Error('Auth failed');

        const data = await response.json();

        // Update UI
        document.getElementById('local-ip').textContent = data.ip;
        document.getElementById('cpu-value').textContent = `${data.cpu.usage}%`;
        document.getElementById('cpu-progress').style.width = `${data.cpu.usage}%`;

        document.getElementById('memory-value').textContent = `${data.memory.percent}%`;
        document.getElementById('memory-progress').style.width = `${data.memory.percent}%`;

        const uptimeH = Math.floor(data.uptime / 3600);
        const uptimeM = Math.floor((data.uptime % 3600) / 60);
        document.getElementById('uptime').textContent = `Uptime: ${uptimeH}h ${uptimeM}m`;

        // Update Charts
        cpuChart.data.datasets[0].data.push(data.cpu.usage);
        cpuChart.data.datasets[0].data.shift();
        cpuChart.update('none');

        memChart.data.datasets[0].data.push(data.memory.percent);
        memChart.data.datasets[0].data.shift();
        memChart.update('none');

        // Update Processes
        const procList = document.getElementById('process-list');
        procList.innerHTML = data.processes.map(p => `
            <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.05);">
                <span style="font-weight: 600;">${p.name} <span style="color: var(--text-gray); font-size: 0.8rem; font-weight: 400;">PID: ${p.pid}</span></span>
                <span style="color: var(--primary-blue); font-weight: 700;">${p.cpuPercent}% CPU</span>
            </div>
        `).join('');

    } catch (err) {
        console.error('Fetch error:', err);
        document.getElementById('health-indicator').innerHTML = '<div class="status-dot" style="background-color: #FF3B30; box-shadow: 0 0 10px #FF3B30;"></div> OFFLINE (Check Token)';
        document.getElementById('health-indicator').style.color = '#FF3B30';
    }
}

setInterval(updateData, 2000);
updateData();

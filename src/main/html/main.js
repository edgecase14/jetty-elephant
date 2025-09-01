import { mrSock } from './mrsock.js'
import { TimeSheetCell } from './customElements/ts-cell.js'
import { ProjectMap } from './customElements/proj-map.js'

// This file sets up a timesheet web application that communicates with a server using Server-Sent Events (SSE).
// It handles displaying and updating timesheet data in real-time.

// Phase 1: Set up SSE connection
const mrsock = new mrSock("/sse2/", document.getElementById("tsc")); // Creates WebSocket-like connection

// Phase 2: Initialize custom web components
TimeSheetCell.init(mrsock);  // Initialize timesheet cell component
ProjectMap.init(mrsock);     // Initialize project map component

// Phase 3: Send initial message to start page build
class CellList {
    dummy;  // Simple object to send as initial message
}
const im = new CellList();
im.dummy = 42;

// Connect and send initial message
mrsock.connect(im);

// Set up approval request button
const req_btn = document.querySelector("button[id='app_req']");
const req_status = document.querySelector("div[id='approve']");
function do_request() {
    req_status.innerText = 'somebody';
}
req_btn.addEventListener('click', do_request());

// Build timesheet header and footer with day columns
const days_header = document.getElementById("mrtimesheet").getElementsByTagName("thead")[0].querySelector("tr[id='days']");
const days_footer = document.getElementById("mrtimesheet").getElementsByTagName("tfoot")[0].querySelector("tr[id='day_totals']");
for (let day = 1; day <= 16; day++) {
    let dh_cell = days_header.insertCell();
    dh_cell.setAttribute('id', "day" + day);
    dh_cell.innerText = "day" + day;
    let df_cell = days_footer.insertCell();
    df_cell.setAttribute('id', "day" + day);
}

const tbody = document.getElementById("mrtimesheet").getElementsByTagName('tbody')[0];

// Server message handlers:

// Handle new timesheet cell data
function gotcells(payload) {
    const row = tbody.querySelector("tr[id='" + payload.projid + "']");
    const day = payload.date.slice(-2);
    const targetCell = row.querySelector("td[id='" + Number(day) + "']");

    // Remove placeholder if exists
    const filler = targetCell.querySelector("div[id='filler']");
    if (filler !== null) {
        targetCell.removeChild(filler);
    }

    // Create and insert new timesheet cell
    let cellElem = document.createElement('ts-cell');
    cellElem.setAttribute('timesheet-id', payload.cellid);
    const addot = targetCell.querySelector("div[id='addot']");
    targetCell.insertBefore(cellElem, addot);
    let br = document.createElement('br');
    targetCell.insertBefore(br, addot);
}

// Handle statutory holiday information
function gotStat(payload) {
    const day = payload.holiday.slice(-2);
    const dh_cell = days_header.querySelector("td[id='day" + Number(day) + "']");
    dh_cell.innerText = "day" + Number(day) + " STAT HOLIDAY " + payload.holiday_name;
    dh_cell.className = "is-stat";
}

// Handle new project row data
function gotrow(payload) {
    let proj_row = tbody.insertRow();
    proj_row.setAttribute('id', payload.projid);
    if (payload.projid === 1) {
        proj_row.setAttribute('style', "background-color: #ffe0ff"); // Highlight non-billable
    }

    // Add job ID cell
    let job_id = proj_row.insertCell();
    job_id.innerHTML = payload.job_id + "<br>PM: ABC";
    let addot = document.createElement('button');
    addot.setAttribute('id', "addot");
    addot.innerText = "Add Overtime";
    job_id.appendChild(addot);

    // Add project name cell
    let job_name = proj_row.insertCell();
    job_name.innerText = payload.pname;

    // Add empty day cells
    for (let day = 1; day <= 16; day++) {
        let te_cell = proj_row.insertCell();
        te_cell.setAttribute('id', day);
        let filler = document.createElement('div');
        filler.setAttribute('id', "filler");
        filler.innerText = "empty cell";
        te_cell.appendChild(filler);
    }
}

// Handle calculation updates
const calcs = document.getElementById("calcs").querySelector("td[id='total']");
function calcUpdate(payload) {
    calcs.innerText = payload.total;
}

// Handle username updates
function showusername(payload) {
    const username = payload.username;
    const un = document.getElementById("username");
    un.innerText = username;
    
    const url = new URL(window.location.href);
    url.searchParams.set('user', username);
    history.replaceState(null, "", url);
}

// Username selection handling
class ShowUsername {
    username; // String
}

const pick_user = document.getElementById("names");
pick_user.addEventListener('change', (e) => {
    const su = new ShowUsername();
    su.username = e.target.value;
    mrsock.send(su);
});

// Handle pay period updates
function showperiod(payload) {
    const pay_period = payload.pay_period;
    const un = document.getElementById("pay_period");
    un.innerText = pay_period;
    
    const url = new URL(window.location.href);
    url.searchParams.set('pay_period', pay_period);
    history.replaceState(null, "", url);
}

// Register all message handlers
mrsock.registerCallback("ShowUsername", showusername);
mrsock.registerCallback("CellAdd", gotcells);
mrsock.registerCallback("GotRow", gotrow);
mrsock.registerCallback("calc-update", calcUpdate);
mrsock.registerCallback("stat-days", gotStat);
mrsock.registerCallback("ShowPeriod", showperiod);

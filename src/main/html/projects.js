import { mrSock } from './mrsock.js'
//import { TimeSheetCell } from './customElements/ts-cell.js'

// phase 1
const mrsock = new mrSock("/sse2/", document.getElementById("tsc"));

// phase 2
//TimeSheetCell.init(mrsock);

// phase 3 - kickoff whole page build process -send Initial Message
class CellList {
    dummy;
}
const im = new CellList();
im.dummy = 42;

// need to delay sending message until <proj-map> gets instantiated, so onmessage doesn't hit a wall

mrsock.connect(im);
//setTimeout(mrsock.connect.bind(mrsock), 5000, im); // events won't be delivered until this script finishes right? microtasks?

// now how do we manage EventSource state machine, to sync UI?  connection lost, session timeout, reconnect to same session, etc.


const tbody = document.getElementById("projects").getElementsByTagName('tbody')[0];

//let queryString = window.location.search;
//let params = new URLSearchParams(queryString);
//window.login = params.get("login");


// onmessage callbacks
function gotcells(payload) {
    console.log(payload);

    // could be more specific, see days_header above
    const row = tbody.querySelector("tr[id='" + payload.projid + "']");
    // is there another attribute besides 'id' we can use?  it pollutes a document global namespace
    const day = payload.date.slice(-2); // get day of month
    const targetCell = row.querySelector("td[id='" + Number(day) + "']");

    const filler = targetCell.querySelector("div[id='filler']");
    if (filler !== null) {
        targetCell.removeChild(filler);
    }

    let cellElem = document.createElement('ts-cell');
    cellElem.setAttribute('timesheet-id', payload.cellid);
    // set attribute for hide-date, has ot, is-stat-day, is mandatory-stat-ot

    const addot = targetCell.querySelector("div[id='addot']");
    targetCell.insertBefore(cellElem, addot);
    let br = document.createElement('br');
    targetCell.insertBefore(br, addot);
}

function gotrow(payload) {
    // can we build the row first, then insert into live DOM?
    let proj_row = tbody.insertRow();
    proj_row.setAttribute('id', payload.projid);
    // TODO - get billable status from backend
    if (payload.projid === 1) {
        // hilight non-billable
        proj_row.setAttribute('style', "background-color: #ffe0ff");
    }

    let job_id = proj_row.insertCell();
    job_id.innerHTML = payload.job_id + "<br>PM: ABC";

    let job_name = proj_row.insertCell();
    job_name.innerText = payload.pname;

    let te_cell = proj_row.insertCell();
    te_cell.setAttribute('id', 'ts_active');

    let filler = document.createElement('div');
    filler.setAttribute('id', "filler");
    filler.innerText = "empty cell";
    te_cell.appendChild(filler);

}

function showusername(payload) {

    //console.log(payload);

    const un = document.getElementById("username");
    un.innerText = payload.user;
}
mrsock.registerCallback("username", showusername);
mrsock.registerCallback("CellAdd", gotcells);
mrsock.registerCallback("GotRow", gotrow);

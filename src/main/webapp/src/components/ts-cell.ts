export class CellUpdate {
    id;
    contents;
    note_str;
}

//import { mrSock } from '../mrsock.js'

import { BaseElement } from './base-element.js'

export class TimeSheetCell extends BaseElement {
    static myCells = new Map();
    static my_elem_name = 'ts-cell';
    static sock;

    static init(mrsock) {
        mrsock.registerCallback("CellUpdate", this.mymessage);
        mrsock.registerCallback("CellAck", this.mymessage);
        this.sock=mrsock;
    }

    // Can define constructor arguments if you wish.
    constructor() {
        super();

        // Create some CSS to apply to the shadow dom
//	const style = document.createElement('style');
//	style.innerHTML = `


        const sheet = new CSSStyleSheet();
        sheet.replaceSync(`
* {
	margin: .5em;
	display: inline-block;
	border: 1px solid #ddd;
	font-family: arial;
	background-color: black;
	color: goldenrod;
}

div {
	padding: 10px;
	max-width: 8em;
	text-overflow: elipsis;
}

.foobar {
	background-color: white;
	color: black;
}

.bloody {
	background-color: red;
	color: white;
}

.rolling-meadows {
	background-color: #88ef99;
	color: black;
}
.is-stat {
	background-color: #68cf79;
	color: black;
`);
        this.shadowRoot.adoptedStyleSheets = [sheet];

        //this.shadowRoot.appendChild(style);

        // Create (nested) span elements
        const wrapper = document.createElement('div');
        //wrapper.setAttribute('id', 'entry_el');
        wrapper.setAttribute('contenteditable', 'true');
        wrapper.innerText = "0";
        this.shadowRoot.appendChild(wrapper);

        const note_el = document.createElement('div');
        note_el.setAttribute('id', 'note_el');
        note_el.setAttribute('contenteditable', 'true');
        note_el.innerText = "notes go here!";
        this.shadowRoot.append(note_el);

        // make this conditional on customElement Attribute, "hide_date"?
        //const date_el = document.createElement('div');
        //date_el.setAttribute('id', 'date_el');
        //date_el.innerText = "YYYY-MM-DD";
        //this.shadowRoot.append(date_el);

        function mykeydown(e) {
            if (e.keyCode === 13) {
                this.shadowRoot.querySelector("div").className = "";
                this.shadowRoot.querySelector("div").className = "bloody";
                e.preventDefault();
                e.stopPropagation();
                this.blur();

                const cu = new CellUpdate();
                cu.id = Number(this.getAttribute("timesheet-id"));
                cu.contents = this.shadowRoot.querySelector("div").innerText;
                cu.note = this.shadowRoot.querySelector("div[id='note_el']").innerText;

                this.constructor.sock.send(cu);

                return false;
            }
        }
        this.addEventListener("keydown", mykeydown.bind(this));

    } // constructor !!

    ack(payload) {
        if (payload.type === "CellAck") {
            this.shadowRoot.querySelector("div").className = "rolling-meadows";
            //const myid = this.getAttribute("timesheet-id");
        }
        if (payload.type === "CellUpdate") {
            this.shadowRoot.querySelector("div").innerText = payload.contents;
            this.shadowRoot.querySelector("div[id='note_el']").innerText = payload.note;
            // make this conditional on customElement Attribute, "hide_date"?
            //this.shadowRoot.querySelector("div[id='date_el']").innerText = payload.date;
        }
    }

    // move to superclass
    static mymessage(payload) {
        if (payload.id === undefined ) {
            console.log("TimeSheetCell: message contained no ID.");
        }
        // also check if it is a number?
        const handler = TimeSheetCell.myCells.get(payload.id);
        if (handler) {
            handler(payload);
        } else {
            console.log("TimeSheetCell: no id matched.");
        }
    }

    // move most to superclass
    connectedCallback() {
        if (this.hasAttribute("timesheet-id")) { // parameterize: $ce_type_name-id
            const myid = Number(this.getAttribute("timesheet-id"));
            // console.log("my id is : " + myid);
            // DOM id format $ce_type_name-id-$myid
            this.shadowRoot.querySelector("div").setAttribute('id', myid); // maybe some kind of introspection instead?
            // like search backwards the first enclosing custom-element (or root of shadow-dom?) and this.getAttribute
            // is this loosing intial message due to race?
            TimeSheetCell.myCells.set(myid, this.ack.bind(this));
        } else {
            console.log("error: ts-cell: attribute timesheet-id is required when element is attached to DOM");
        }
    }

}

customElements.define('ts-cell', TimeSheetCell);

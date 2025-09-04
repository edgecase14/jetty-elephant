/**
 * A WebSocket-like class that uses Server-Sent Events (EventSource) for real-time communication
 * 
 * This class provides:
 * - One-way server->client messaging using SSE/EventSource
 * - Client->server messaging using fetch POST requests
 * - JSON message parsing and dispatch to registered callbacks
 * - Optional connection status display element
 * - Automatic reconnection handling
 */
export class mrSock {

    url;                    // The SSE endpoint URL
    listeners;              // Map of message type -> callback handlers
    statusElement;          // Optional DOM element to show connection status
    onOpenMsg;             // Message to send when connection first opens
    evtSource;             // The EventSource instance
    open_sent = false;     // Track if initial open message was sent

    constructor(url, sockStatus) {
        this.url = url;
        this.listeners = new Map();

        // Create optional status display element
        if (sockStatus) {
            this.statusElement = document.createElement('p');
            this.statusElement.innerText = url + " setup";
            sockStatus.appendChild(this.statusElement);
        }
    }

    connect(openmsg) {
        this.onOpenMsg = openmsg;
        this.evtSource = new EventSource(this.url);

        // Handle connection open
        const myonopen = function (event) {
            if (this.statusElement) {
                this.statusElement.innerText = this.url + " open";
            }
            // Send initial message only once
            if (this.open_sent === false) {
                this.send(this.onOpenMsg);
                this.open_sent = true;
            }
        };
        this.evtSource.addEventListener("open", myonopen.bind(this));

        // Handle connection errors
        const myonerror = function (event) {
            if (this.statusElement) {
                this.statusElement.innerText = this.url + " error";
            }
        };
        this.evtSource.onerror = myonerror.bind(this);

        // Handle incoming messages
        const onmsg = function (event) {
            if (event.data == null) {
                console.log("empty event");
                return;
            };
            const jsondata = JSON.parse(event.data);
            if (jsondata === null) {
                console.log("empty json: " + event.data);
                return;
            };
            // Handle both single messages and arrays of messages
            if (Array.isArray(jsondata)) {
                for (let amsg of jsondata) {
                    this.dispatchOne(amsg);
                }
            } else {
                this.dispatchOne(jsondata);
            }
        };
        this.evtSource.onmessage = onmsg.bind(this);
    }

    // Dispatch a single message to its registered handler
    dispatchOne(updatemsg) {
        if (updatemsg.type === "Reload") {
            window.location.reload();
            return;
        }
        let handler = this.listeners.get(updatemsg.type);
        if (handler) {
            handler(updatemsg);
        } else {
            console.log("unregeistered tsc event type " + updatemsg.type.toString());
        }
    }

    // Send a message to the server via POST
    send(updatemsg) {
        updatemsg.type = updatemsg.constructor.name;
        let jsn = JSON.stringify(updatemsg);
        fetch('tsc2/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: jsn
        });
    }

    // Register a callback handler for a specific message type
    registerCallback(type, cb) {
        this.listeners.set(type, cb);
    }
}

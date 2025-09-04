/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/javascript.js to edit this template
 */


export class BaseElement extends HTMLElement {
    constructor() {
        // If you define a constructor, always call super() first!
        // This is specific to CE and required by the spec.
        super();

        // Create a shadow root
        this.attachShadow({mode: 'open'}); // sets and returns 'this.shadowRoot'
    }
}
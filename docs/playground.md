---
hide:
- title
- navigation
- toc
- footer
---

<style>
      /* see: https://github.com/squidfunk/mkdocs-material/issues/2163#issuecomment-752916358 */
    .md-typeset h1, .md-content__button {
      display: none;
    }
    .md-grid {
        margin-top: 0;
        margin-bottom: 0;
        height: 100%;
    }
    .container {
        /* see: https://stackoverflow.com/a/35676852/4814697*/
        display: flex;
        
        /* see: https://css-tricks.com/full-width-containers-limited-width-parents */
        width: 100vw;
        position: relative;
        left: 50%;
        right: 50%;
        margin-left: -50vw; 
        margin-right: -50vw;
    }
    .container > div {
        flex: 1; /*grow*/
        max-width: 33%;
    }
    .editor-container {
        display: flex;
        flex-direction: column;
    }
    .editor-container > h3 {
        margin-top: 0;
        text-align: center;
    }
    .editor {
        flex: 1;
    }
    /* see: view-source:https://codemirror.net/try/ */
    .cm-editor {
        height: 100%;
        max-height: none;
        border: none;
    }
    .cm-scroller {
        overflow: auto;
    }
</style>
<div class="container">
    <div class="editor-container">
        <h3>Input</h3>
        <div id="in-editor" class="editor"></div>
    </div>
    <div class="editor-container">
        <h3>xtrasonnet</h3>
        <div id="xtr-editor" class="editor"></div>
    </div>
    <div class="editor-container">
        <h3>Output</h3>
        <div id="out-editor" class="editor"></div>
    </div>
</div>

<script src="https://unpkg.com/rxjs@^7/dist/bundles/rxjs.umd.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/js-beautify/1.14.7/beautify.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/js-beautify/1.14.7/beautify-html.min.js"></script>
<script src=../javascripts/editor.bundle.js></script>

'use strict';

let API = null;
let currentTab    = 'classes';
let currentCtab   = 'detail';
let currentFilter = 'all';
let currentSearch = '';
let currentClass  = null;
let filteredResults = [];
let methodSearch  = '';
let fieldSearch   = '';

const sourceCache  = {};
let localDirHandle = null;

// Global functions group folding
const foldedGlobalGroups = new Set();

// Class namespace tree folding
const foldedPackages = new Set(); // manually closed
const openedPackages = new Set(); // manually opened beyond foldDepth
let foldDepth = Infinity;         // Infinity = fully open

// Detail panel: whether non-callable method groups are shown
let showNonCallable = false;

// Simple-name → [fqn, …] lookup for source class-ref linking
let classBySimpleName = {};

// Simple-name → relative .java path for classes in _source_index (not in API)
let sourceOnlyPaths = {};

// Navigation history
const navHistory = [];
let navIndex = -1;
let navSeq   = 0; // incremented on each applyState call; guards against stale async nav

// Set to true during applyState so that navPush becomes a no-op.
// This means every nav-aware function can push unconditionally;
// only applyState (history restoration) suppresses the push.
let _restoringState = false;

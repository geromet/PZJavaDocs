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

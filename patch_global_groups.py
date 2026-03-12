"""
Adds a 'group' field to each global_function entry in lua_api.json.
Groups are assigned by explicit function-name lookup so they remain stable
across API versions (new/removed functions won't shift existing assignments).

Re-run this after regenerating lua_api.json.  New functions that don't appear
in FUNCTION_GROUPS will fall into "Other" and be reported as warnings.
"""
import json, pathlib

VIEWER_DIR = pathlib.Path(__file__).parent
API_FILE   = VIEWER_DIR / "lua_api.json"

# Explicit mapping: lua_name → group.
# Overloaded methods (same lua_name, multiple signatures) all share one group.
FUNCTION_GROUPS = {
    # 3D Model Loading
    'loadVehicleModel':              '3D Model Loading',
    'loadStaticZomboidModel':        '3D Model Loading',
    'loadSkinnedZomboidModel':       '3D Model Loading',
    'loadZomboidModel':              '3D Model Loading',
    'setModelMetaData':              '3D Model Loading',
    'reloadModelsMatching':          '3D Model Loading',

    # Admin: Bans & Tickets
    'sledgeDestroy':                 'Admin: Bans & Tickets',
    'getBannedIPs':                  'Admin: Bans & Tickets',
    'getBannedSteamIDs':             'Admin: Bans & Tickets',
    'getTickets':                    'Admin: Bans & Tickets',
    'addTicket':                     'Admin: Bans & Tickets',
    'viewedTicket':                  'Admin: Bans & Tickets',
    'removeTicket':                  'Admin: Bans & Tickets',
    'setAdmin':                      'Admin: Bans & Tickets',
    'addWarningPoint':               'Admin: Bans & Tickets',

    # Animal Hutch & Transport
    'getHutch':                              'Animal Hutch & Transport',
    'getAnimal':                             'Animal Hutch & Transport',
    'sendAddAnimalFromHandsInTrailer':        'Animal Hutch & Transport',
    'sendAddAnimalInTrailer':                'Animal Hutch & Transport',
    'sendRemoveAnimalFromTrailer':           'Animal Hutch & Transport',
    'sendRemoveAndGrabAnimalFromTrailer':    'Animal Hutch & Transport',
    'sendPickupAnimal':                      'Animal Hutch & Transport',
    'sendButcherAnimal':                     'Animal Hutch & Transport',
    'sendFeedAnimalFromHand':                'Animal Hutch & Transport',
    'sendHutchGrabAnimal':                   'Animal Hutch & Transport',
    'sendHutchGrabCorpseAction':             'Animal Hutch & Transport',
    'sendHutchRemoveAnimalAction':           'Animal Hutch & Transport',
    'sendCorpse':                            'Animal Hutch & Transport',

    # Animal Spawning
    'sendAnimalGenome':              'Animal Spawning',
    'addAnimal':                     'Animal Spawning',
    'removeAnimal':                  'Animal Spawning',
    'getAnimalChunk':                'Animal Spawning',

    # Authentication
    'sendSecretKey':                 'Authentication',
    'stopSendSecretKey':             'Authentication',
    'generateSecretKey':             'Authentication',
    'sendGoogleAuth':                'Authentication',
    'createQRCodeTex':               'Authentication',

    # Character Customization
    'getAllOutfits':                  'Character Customization',
    'getAllVehicles':                 'Character Customization',
    'getAllHairStyles':               'Character Customization',
    'getHairStylesInstance':         'Character Customization',
    'getBeardStylesInstance':        'Character Customization',
    'getAllBeardStyles':              'Character Customization',
    'getVoiceStylesInstance':        'Character Customization',
    'getAllVoiceStyles':              'Character Customization',
    'getAllItemsForBodyLocation':     'Character Customization',
    'getAllDecalNamesForItem':        'Character Customization',

    # Chat Processing
    'proceedPM':                     'Chat Processing',
    'processSayMessage':             'Chat Processing',
    'processGeneralMessage':         'Chat Processing',
    'processShoutMessage':           'Chat Processing',
    'proceedFactionMessage':         'Chat Processing',
    'processSafehouseMessage':       'Chat Processing',
    'processAdminChatMessage':       'Chat Processing',
    'showWrongChatTabMessage':       'Chat Processing',
    'focusOnTab':                    'Chat Processing',
    'updateChatSettings':            'Chat Processing',
    'checkPlayerCanUseChat':         'Chat Processing',
    'detectBadWords':                'Chat Processing',
    'profanityFilterCheck':          'Chat Processing',
    'showDebugInfoInChat':           'Chat Processing',

    # Client/Server Commands
    'sendClientCommand':             'Client/Server Commands',
    'sendServerCommand':             'Client/Server Commands',
    'sendServerCommandV':            'Client/Server Commands',
    'sendClientCommandV':            'Client/Server Commands',
    'addVariableToSyncList':         'Client/Server Commands',
    'getOnlineUsername':             'Client/Server Commands',
    'isValidUserName':               'Client/Server Commands',
    'getHourMinute':                 'Client/Server Commands',
    'SendCommandToServer':           'Client/Server Commands',
    'isAdmin':                       'Client/Server Commands',
    'canModifyPlayerScoreboard':     'Client/Server Commands',
    'isAccessLevel':                 'Client/Server Commands',
    'sendHumanVisual':               'Client/Server Commands',
    'stopFire':                      'Client/Server Commands',

    # Container Operations
    'sendAddItemToContainer':        'Container Operations',
    'sendAddItemsToContainer':       'Container Operations',
    'sendAttachedItem':              'Container Operations',
    'sendReplaceItemInContainer':    'Container Operations',
    'sendRemoveItemFromContainer':   'Container Operations',
    'sendRemoveItemsFromContainer':  'Container Operations',
    'replaceItemInContainer':        'Container Operations',
    'sendItemsInContainer':          'Container Operations',

    # Controller Settings
    'getControllerCount':            'Controller Settings',
    'isControllerConnected':         'Controller Settings',
    'getControllerGUID':             'Controller Settings',
    'getControllerName':             'Controller Settings',
    'getControllerAxisCount':        'Controller Settings',
    'getControllerAxisValue':        'Controller Settings',
    'getControllerDeadZone':         'Controller Settings',
    'setControllerDeadZone':         'Controller Settings',
    'saveControllerSettings':        'Controller Settings',
    'getControllerButtonCount':      'Controller Settings',
    'getControllerPovX':             'Controller Settings',
    'getControllerPovY':             'Controller Settings',
    'reloadControllerConfigFiles':   'Controller Settings',
    'isXBOXController':              'Controller Settings',
    'isPlaystationController':       'Controller Settings',

    # Coordinate Conversion
    'tabToX':                        'Coordinate Conversion',
    'istype':                        'Coordinate Conversion',
    'isoToScreenX':                  'Coordinate Conversion',
    'isoToScreenY':                  'Coordinate Conversion',
    'screenToIsoX':                  'Coordinate Conversion',
    'screenToIsoY':                  'Coordinate Conversion',
    'getDirectionTo':                'Coordinate Conversion',

    # Debug Editor Views
    'toggleVehicleRenderToTexture':  'Debug Editor Views',
    'getAnimationViewerState':       'Debug Editor Views',
    'getAttachmentEditorState':      'Debug Editor Views',
    'getEditVehicleState':           'Debug Editor Views',
    'getSpriteModelEditorState':     'Debug Editor Views',
    'showAnimationViewer':           'Debug Editor Views',
    'showAttachmentEditor':          'Debug Editor Views',
    'showChunkDebugger':             'Debug Editor Views',
    'getTileGeometryState':          'Debug Editor Views',
    'showGlobalObjectDebugger':      'Debug Editor Views',
    'showSeamEditor':                'Debug Editor Views',
    'getSeamEditorState':            'Debug Editor Views',
    'showSpriteModelEditor':         'Debug Editor Views',
    'showVehicleEditor':             'Debug Editor Views',
    'showWorldMapEditor':            'Debug Editor Views',

    # Debug Rendering
    'debugSetRoomType':              'Debug Rendering',
    'renderIsoCircle':               'Debug Rendering',
    'renderIsoRect':                 'Debug Rendering',
    'renderLine':                    'Debug Rendering',
    'configureLighting':             'Debug Rendering',
    'invalidateLighting':            'Debug Rendering',
    'addAreaHighlight':              'Debug Rendering',
    'addAreaHighlightForPlayer':     'Debug Rendering',

    # Debug Tools
    'getSpecificPlayer':             'Debug Tools',
    'getCameraOffX':                 'Debug Tools',
    'getLatestSave':                 'Debug Tools',
    'isCurrentExecutionPoint':       'Debug Tools',
    'toggleBreakOnChange':           'Debug Tools',
    'isDebugEnabled':                'Debug Tools',
    'toggleBreakOnRead':             'Debug Tools',
    'toggleBreakpoint':              'Debug Tools',
    'getDebugOptions':               'Debug Tools',
    'testHelicopter':                'Debug Tools',
    'endHelicopter':                 'Debug Tools',
    'emulateAnimEvent':              'Debug Tools',
    'emulateAnimEventOnce':          'Debug Tools',
    'takeScreenshot':                'Debug Tools',
    'log':                           'Debug Tools',
    'sendDebugStory':                'Debug Tools',

    # Debugger & Lua Inspector
    'assaultPlayer':                 'Debugger & Lua Inspector',
    'isoRegionsRenderer':            'Debugger & Lua Inspector',
    'zpopNewRenderer':               'Debugger & Lua Inspector',
    'zpopSpawnTimeToZero':           'Debugger & Lua Inspector',
    'zpopClearZombies':              'Debugger & Lua Inspector',
    'zpopSpawnNow':                  'Debugger & Lua Inspector',
    'addVirtualZombie':              'Debugger & Lua Inspector',
    'luaDebug':                      'Debugger & Lua Inspector',
    'setAggroTarget':                'Debugger & Lua Inspector',
    'debugFullyStreamedIn':          'Debugger & Lua Inspector',
    'getClassFieldVal':              'Debugger & Lua Inspector',
    'getMethodParameter':            'Debugger & Lua Inspector',
    'getMethodParameterCount':       'Debugger & Lua Inspector',
    'breakpoint':                    'Debugger & Lua Inspector',
    'getLuaDebuggerErrorCount':      'Debugger & Lua Inspector',
    'getLuaDebuggerErrors':          'Debugger & Lua Inspector',
    'doLuaDebuggerAction':           'Debugger & Lua Inspector',
    'isQuitCooldown':                'Debugger & Lua Inspector',
    'debugLuaTable':                 'Debugger & Lua Inspector',
    'displayLUATable':               'Debugger & Lua Inspector',

    # Entity & Script Reload
    'reloadVehicles':                'Entity & Script Reload',
    'reloadEngineRPM':               'Entity & Script Reload',
    'reloadXui':                     'Entity & Script Reload',
    'reloadScripts':                 'Entity & Script Reload',
    'reloadEntityScripts':           'Entity & Script Reload',
    'reloadEntitiesDebug':           'Entity & Script Reload',
    'reloadEntityDebug':             'Entity & Script Reload',
    'reloadEntityFromScriptDebug':   'Entity & Script Reload',
    'getIsoEntitiesDebug':           'Entity & Script Reload',
    'reloadVehicleTextures':         'Entity & Script Reload',

    # Environment & Climate
    'useStaticErosionRand':          'Environment & Climate',
    'getClimateManager':             'Environment & Climate',
    'getClimateMoon':                'Environment & Climate',
    'getWorldMarkers':               'Environment & Climate',
    'getIsoMarkers':                 'Environment & Climate',
    'getErosion':                    'Environment & Climate',
    'rainConfig':                    'Environment & Climate',
    'updateFire':                    'Environment & Climate',
    'forceSnowCheck':                'Environment & Climate',

    # Event System
    'triggerEvent':                  'Event System',

    # Factions & Safehouses
    'sendFactionInvite':             'Factions & Safehouses',
    'acceptFactionInvite':           'Factions & Safehouses',
    'sendSafehouseInvite':           'Factions & Safehouses',
    'acceptSafehouseInvite':         'Factions & Safehouses',
    'sendSafehouseChangeMember':     'Factions & Safehouses',
    'sendSafehouseChangeOwner':      'Factions & Safehouses',
    'sendSafehouseChangeRespawn':    'Factions & Safehouses',
    'sendSafehouseChangeTitle':      'Factions & Safehouses',
    'sendSafezoneClaim':             'Factions & Safehouses',
    'sendSafehouseClaim':            'Factions & Safehouses',
    'sendSafehouseRelease':          'Factions & Safehouses',

    # File I/O
    'lineSeparator':                 'File I/O',
    'getFileWriter':                 'File I/O',
    'getSandboxFileWriter':          'File I/O',
    'createStory':                   'File I/O',
    'createWorld':                   'File I/O',
    'sanitizeWorldName':             'File I/O',
    'forceChangeState':              'File I/O',
    'endFileOutput':                 'File I/O',
    'getFileInput':                  'File I/O',
    'getGameFilesInput':             'File I/O',
    'getGameFilesTextInput':         'File I/O',
    'endTextFileInput':              'File I/O',
    'endFileInput':                  'File I/O',
    'getLineNumber':                 'File I/O',
    'ZombRand':                      'File I/O',
    'ZombRandBetween':               'File I/O',
    'ZombRandFloat':                 'File I/O',
    'getShortenedFilename':          'File I/O',
    'getFileSeparator':              'File I/O',

    # File System
    'serverFileExists':              'File System',
    'checkSaveFolderExists':         'File System',
    'getAbsoluteSaveFolderName':     'File System',
    'checkSaveFileExists':           'File System',
    'checkSavePlayerExists':         'File System',
    'cacheFileExists':               'File System',
    'fileExists':                    'File System',

    # Game Actions
    'createBuildAction':             'Game Actions',
    'startFishingAction':            'Game Actions',
    'getPickedUpFish':               'Game Actions',

    # Game Client
    'getGameClient':                 'Game Client',
    'isCoopHost':                    'Game Client',
    'disconnect':                    'Game Client',
    'writeLog':                      'Game Client',

    # Game Configuration
    'getCustomizationData':          'Game Configuration',
    'getCombatConfig':               'Game Configuration',
    'setMinMaxZombiesPerChunk':      'Game Configuration',
    'setShowPausedMessage':          'Game Configuration',
    'getSandboxOptions':             'Game Configuration',
    'getSandboxPresets':             'Game Configuration',
    'deleteSandboxPreset':           'Game Configuration',

    # Game Saves
    'save':                          'Game Saves',
    'saveGame':                      'Game Saves',

    # Game Speed & Pause
    'getGameSpeed':                  'Game Speed & Pause',
    'setGameSpeed':                  'Game Speed & Pause',
    'stepForward':                   'Game Speed & Pause',
    'isGamePaused':                  'Game Speed & Pause',

    # Gamepad Input
    'isJoypadPressed':                       'Gamepad Input',
    'isJoypadDown':                          'Gamepad Input',
    'isJoypadLTPressed':                     'Gamepad Input',
    'isJoypadRTPressed':                     'Gamepad Input',
    'isJoypadLeftStickButtonPressed':        'Gamepad Input',
    'isJoypadRightStickButtonPressed':       'Gamepad Input',
    'getJoypadAimingAxisX':                  'Gamepad Input',
    'getJoypadAimingAxisY':                  'Gamepad Input',
    'getJoypadMovementAxisX':                'Gamepad Input',
    'getJoypadMovementAxisY':                'Gamepad Input',
    'getJoypadAButton':                      'Gamepad Input',
    'getJoypadBButton':                      'Gamepad Input',
    'getJoypadXButton':                      'Gamepad Input',
    'getJoypadYButton':                      'Gamepad Input',
    'getJoypadLBumper':                      'Gamepad Input',
    'getJoypadRBumper':                      'Gamepad Input',
    'getJoypadBackButton':                   'Gamepad Input',
    'getJoypadStartButton':                  'Gamepad Input',
    'getJoypadLeftStickButton':              'Gamepad Input',
    'getJoypadRightStickButton':             'Gamepad Input',
    'wasMouseActiveMoreRecentlyThanJoypad':  'Gamepad Input',
    'activateJoypadOnSteamDeck':             'Gamepad Input',
    'reactivateJoypadAfterResetLua':         'Gamepad Input',
    'isJoypadConnected':                     'Gamepad Input',
    'toInt':                                 'Gamepad Input',
    'getClientUsername':                     'Gamepad Input',
    'setPlayerJoypad':                       'Gamepad Input',
    'setPlayerMouse':                        'Gamepad Input',
    'revertToKeyboardAndMouse':              'Gamepad Input',
    'revertToKeyboardAndMouseFromMainMenu':  'Gamepad Input',
    'isJoypadUp':                            'Gamepad Input',
    'isJoypadLeft':                          'Gamepad Input',
    'isJoypadRight':                         'Gamepad Input',
    'isJoypadLBPressed':                     'Gamepad Input',
    'isJoypadRBPressed':                     'Gamepad Input',
    'getButtonCount':                        'Gamepad Input',
    'setDebugToggleControllerPluggedIn':     'Gamepad Input',

    # Hit & Combat Events
    'getFakeAttacker':               'Hit & Combat Events',
    'sendHitPlayer':                 'Hit & Combat Events',
    'sendHitVehicle':                'Hit & Combat Events',

    # Inventory Management
    'sendRequestInventory':          'Inventory Management',
    'InvMngGetItem':                 'Inventory Management',
    'InvMngRemoveItem':              'Inventory Management',
    'InvMngUpdateItem':              'Inventory Management',

    # Item Information
    'getAllItems':                   'Item Information',
    'getAllRecipes':                  'Item Information',
    'getEvolvedRecipes':             'Item Information',
    'getItemNameFromFullType':       'Item Information',
    'getItem':                       'Item Information',
    'getItemStaticModel':            'Item Information',
    'isItemFood':                    'Item Information',
    'getItemFoodType':               'Item Information',
    'isItemFresh':                   'Item Information',
    'getItemCount':                  'Item Information',
    'getItemWeight':                 'Item Information',
    'getItemActualWeight':           'Item Information',
    'getItemConditionMax':           'Item Information',
    'getItemEvolvedRecipeName':      'Item Information',
    'hasItemTag':                    'Item Information',
    'getItemDisplayName':            'Item Information',
    'getItemName':                   'Item Information',
    'getItemTextureName':            'Item Information',
    'getAndFindNearestTracks':       'Item Information',
    'getItemTex':                    'Item Information',
    'getRecipeDisplayName':          'Item Information',

    # Item Scripting
    'instanceItem':                  'Item Scripting',
    'createNewScriptItem':           'Item Scripting',
    'cloneItemType':                 'Item Scripting',
    'moduleDotType':                 'Item Scripting',

    # Item Sync
    'syncItemActivated':             'Item Sync',
    'syncItemModData':               'Item Sync',
    'syncItemFields':                'Item Sync',
    'syncHandWeaponFields':          'Item Sync',

    # Item Transactions & Actions
    'createItemTransaction':             'Item Transactions & Actions',
    'createItemTransactionWithPosData':  'Item Transactions & Actions',
    'changeItemTypeTransaction':         'Item Transactions & Actions',
    'removeItemTransaction':             'Item Transactions & Actions',
    'isItemTransactionConsistent':       'Item Transactions & Actions',
    'isItemTransactionDone':             'Item Transactions & Actions',
    'isItemTransactionRejected':         'Item Transactions & Actions',
    'getItemTransactionDuration':        'Item Transactions & Actions',
    'isActionDone':                      'Item Transactions & Actions',
    'isActionRejected':                  'Item Transactions & Actions',
    'getActionDuration':                 'Item Transactions & Actions',
    'removeAction':                      'Item Transactions & Actions',

    # Java Reflection
    'getNumClassFunctions':                  'Java Reflection',
    'getClassFunction':                      'Java Reflection',
    'getNumClassFields':                     'Java Reflection',
    'getClassField':                         'Java Reflection',

    # Key Events
    'getKeyName':                    'Key Events',
    'getKeyCode':                    'Key Events',
    'queueCharEvent':                'Key Events',
    'queueKeyEvent':                 'Key Events',

    # Keyboard Input
    'isKeyDown':                     'Keyboard Input',
    'wasKeyDown':                    'Keyboard Input',
    'isKeyPressed':                  'Keyboard Input',
    'isShiftKeyDown':                'Keyboard Input',
    'isCtrlKeyDown':                 'Keyboard Input',
    'isAltKeyDown':                  'Keyboard Input',
    'isMetaKeyDown':                 'Keyboard Input',
    'doKeyPress':                    'Keyboard Input',

    # Lua File I/O
    'getFileOutput':                         'Lua File I/O',
    'getLastStandPlayersDirectory':          'Lua File I/O',
    'getLastStandPlayerFileNames':           'Lua File I/O',
    'getAllSavedPlayers':                    'Lua File I/O',
    'getFileReader':                         'Lua File I/O',
    'getModFileReader':                      'Lua File I/O',
    'listFilesInZomboidLuaDirectory':        'Lua File I/O',
    'listFilesInModDirectory':               'Lua File I/O',
    'getModFileWriter':                      'Lua File I/O',
    'getMyDocumentFolder':                   'Lua File I/O',

    # Lua Runtime
    'require':                       'Lua Runtime',
    'getRenderer':                   'Lua Runtime',
    'getGameTime':                   'Lua Runtime',
    'getMaxPlayers':                 'Lua Runtime',
    'callLua':                       'Lua Runtime',
    'callLuaReturn':                 'Lua Runtime',
    'callLuaBool':                   'Lua Runtime',
    'getCore':                       'Lua Runtime',

    # Lua Stack Introspection
    'getFilenameOfCallframe':        'Lua Stack Introspection',
    'getFilenameOfClosure':          'Lua Stack Introspection',
    'getFirstLineOfClosure':         'Lua Stack Introspection',
    'getLocalVarCount':              'Lua Stack Introspection',
    'getLocalVarName':               'Lua Stack Introspection',
    'getLocalVarStack':              'Lua Stack Introspection',
    'getLocalVarStackIndex':         'Lua Stack Introspection',
    'getCallframeTop':               'Lua Stack Introspection',
    'getCoroutineTop':               'Lua Stack Introspection',
    'getCoroutineObjStack':          'Lua Stack Introspection',
    'getCoroutineObjStackWithBase':  'Lua Stack Introspection',
    'localVarName':                  'Lua Stack Introspection',
    'getCoroutineCallframeStack':    'Lua Stack Introspection',
    'getLuaStackTrace':              'Lua Stack Introspection',

    # Miscellaneous Utilities
    'getBehaviourDebugPlayer':       'Miscellaneous Utilities',
    'setBehaviorStep':               'Miscellaneous Utilities',
    'getPuddlesManager':             'Miscellaneous Utilities',
    'getAllAnimalsDefinitions':       'Miscellaneous Utilities',
    'setPuddles':                    'Miscellaneous Utilities',
    'fastfloor':                     'Miscellaneous Utilities',
    'getRandomUUID':                 'Miscellaneous Utilities',
    'sendItemListNet':               'Miscellaneous Utilities',
    'convertToPZNetTable':           'Miscellaneous Utilities',
    'instanceof':                    'Miscellaneous Utilities',
    'getClassSimpleName':            'Miscellaneous Utilities',
    'transformIntoKahluaTable':      'Miscellaneous Utilities',
    'copyTable':                     'Miscellaneous Utilities',
    'timSort':                       'Miscellaneous Utilities',
    'javaListRemoveAt':              'Miscellaneous Utilities',
    'getSearchMode':                 'Miscellaneous Utilities',
    'transmitBigWaterSplash':        'Miscellaneous Utilities',
    'replaceWith':                   'Miscellaneous Utilities',

    # Mod Management
    'isModActive':                   'Mod Management',
    'openUrl':                       'Mod Management',
    'isDesktopOpenSupported':        'Mod Management',
    'showFolderInDesktop':           'Mod Management',
    'getActivatedMods':              'Mod Management',
    'toggleModActive':               'Mod Management',
    'saveModsFile':                  'Mod Management',
    'manipulateSavefile':            'Mod Management',
    'getServerModData':              'Mod Management',
    'checkModsNeedUpdate':           'Mod Management',

    # Mod Utilities
    'getModDirectoryTable':          'Mod Utilities',
    'getMapFoldersForMod':           'Mod Utilities',
    'spawnpointsExistsForMod':       'Mod Utilities',
    'getScriptManager':              'Mod Utilities',

    # Mouse Input
    'getMouseXScaled':               'Mouse Input',
    'getMouseYScaled':               'Mouse Input',
    'getMouseX':                     'Mouse Input',
    'setMouseXY':                    'Mouse Input',
    'isMouseButtonDown':             'Mouse Input',
    'isMouseButtonPressed':          'Mouse Input',
    'getMouseY':                     'Mouse Input',

    # Multiplayer Connection
    'ping':                          'Multiplayer Connection',
    'stopPing':                      'Multiplayer Connection',
    'serverConnect':                 'Multiplayer Connection',
    'serverConnectCoop':             'Multiplayer Connection',
    'sendPing':                      'Multiplayer Connection',
    'connectionManagerLog':          'Multiplayer Connection',
    'forceDisconnect':               'Multiplayer Connection',
    'checkPermissions':              'Multiplayer Connection',
    'backToSinglePlayer':            'Multiplayer Connection',
    'isIngameState':                 'Multiplayer Connection',
    'getPerformanceLocal':           'Multiplayer Connection',
    'getNetworkLocal':               'Multiplayer Connection',
    'getGameLocal':                  'Multiplayer Connection',
    'getPerformanceRemote':          'Multiplayer Connection',
    'getNetworkRemote':              'Multiplayer Connection',
    'getGameRemote':                 'Multiplayer Connection',
    'getMPStatus':                   'Multiplayer Connection',
    'canConnect':                    'Multiplayer Connection',
    'getReconnectCountdownTimer':    'Multiplayer Connection',

    # Overhead Map
    'translatePointXInOverheadMapToWindow':  'Overhead Map',
    'translatePointYInOverheadMapToWindow':  'Overhead Map',
    'translatePointXInOverheadMapToWorld':   'Overhead Map',
    'translatePointYInOverheadMapToWorld':   'Overhead Map',
    'drawOverheadMap':                       'Overhead Map',

    # Player Data Sync
    'sendVisual':                    'Player Data Sync',
    'sendSyncPlayerFields':          'Player Data Sync',
    'sendClothing':                  'Player Data Sync',
    'syncVisuals':                   'Player Data Sync',
    'sendEquip':                     'Player Data Sync',
    'sendDamage':                    'Player Data Sync',
    'sendPlayerEffects':             'Player Data Sync',
    'sendItemStats':                 'Player Data Sync',
    'hasDataReadBreakpoint':         'Player Data Sync',
    'hasDataBreakpoint':             'Player Data Sync',
    'hasBreakpoint':                 'Player Data Sync',
    'sendPlayerStatsChange':         'Player Data Sync',
    'sendPersonalColor':             'Player Data Sync',

    # Player Database
    'deletePlayerFromDatabase':      'Player Database',
    'checkPlayerExistsInDatabase':   'Player Database',
    'deletePlayerSave':              'Player Database',

    # Player Management
    'getSleepingEvent':              'Player Management',
    'setPlayerMovementActive':       'Player Management',
    'setActivePlayer':               'Player Management',
    'getPlayer':                     'Player Management',
    'getNumActivePlayers':           'Player Management',
    'getMaxActivePlayers':           'Player Management',
    'getPlayerScreenLeft':           'Player Management',
    'getPlayerScreenTop':            'Player Management',
    'getPlayerScreenWidth':          'Player Management',
    'getPlayerScreenHeight':         'Player Management',
    'getPlayerByOnlineID':           'Player Management',
    'initUISystem':                  'Player Management',
    'getConnectedPlayers':           'Player Management',
    'getPlayerFromUsername':         'Player Management',
    'teleportPlayers':               'Player Management',

    # Radio & Broadcast
    'getRadioAPI':                   'Radio & Broadcast',
    'getRadioTranslators':           'Radio & Broadcast',
    'getTranslatorCredits':          'Radio & Broadcast',
    'getZomboidRadio':               'Radio & Broadcast',

    # Rendering & Performance
    'getPerformance':                'Rendering & Performance',
    'setZoomLevels':                 'Rendering & Performance',
    'screenZoomIn':                  'Rendering & Performance',
    'screenZoomOut':                 'Rendering & Performance',
    'Render3DItem':                  'Rendering & Performance',
    'getContainerOverlays':          'Rendering & Performance',
    'getTileOverlays':               'Rendering & Performance',
    'NewMapBinaryFile':              'Rendering & Performance',
    'getAverageFPS':                 'Rendering & Performance',
    'getCPUTime':                    'Rendering & Performance',
    'getGPUTime':                    'Rendering & Performance',
    'getCPUWait':                    'Rendering & Performance',
    'getGPUWait':                    'Rendering & Performance',
    'getServerFPS':                  'Rendering & Performance',
    'configRoomFade':                'Rendering & Performance',

    # Save Info
    'getLastPlayedDate':             'Save Info',
    'getTextureFromSaveDir':         'Save Info',
    'getSaveInfo':                   'Save Info',
    'renameSavefile':                'Save Info',
    'setSavefilePlayer1':            'Save Info',
    'getServerSavedWorldVersion':    'Save Info',

    # Save Management
    'getSaveDirectory':              'Save Management',
    'getFullSaveDirectoryTable':     'Save Management',
    'getSaveDirectoryTable':         'Save Management',
    'getCurrentSaveName':            'Save Management',
    'doChallenge':                   'Save Management',
    'doTutorial':                    'Save Management',
    'deleteAllGameModeSaves':        'Save Management',

    # Server / Client State
    'getLoadedLuaCount':             'Server / Client State',
    'getLoadedLua':                  'Server / Client State',
    'isServer':                      'Server / Client State',
    'isServerSoftReset':             'Server / Client State',
    'isClient':                      'Server / Client State',
    'isMultiplayer':                 'Server / Client State',
    'canSeePlayerStats':             'Server / Client State',
    'getAccessLevel':                'Server / Client State',
    'haveAccess':                    'Server / Client State',
    'getOnlinePlayers':              'Server / Client State',
    'getDebug':                      'Server / Client State',
    'getCameraOffY':                 'Server / Client State',

    # Server Config & Runtime
    'isDemo':                        'Server Config & Runtime',
    'getTimeInMillis':               'Server Config & Runtime',
    'getCurrentCoroutine':           'Server Config & Runtime',
    'reloadLuaFile':                 'Server Config & Runtime',
    'reloadServerLuaFile':           'Server Config & Runtime',
    'setSpawnRegion':                'Server Config & Runtime',
    'getServerSpawnRegions':         'Server Config & Runtime',
    'getServerOptions':              'Server Config & Runtime',
    'getServerName':                 'Server Config & Runtime',
    'getServerIP':                   'Server Config & Runtime',
    'getServerPort':                 'Server Config & Runtime',
    'isShowConnectionInfo':          'Server Config & Runtime',
    'setShowConnectionInfo':         'Server Config & Runtime',
    'isShowServerInfo':              'Server Config & Runtime',
    'setShowServerInfo':             'Server Config & Runtime',
    'getServerSettingsManager':      'Server Config & Runtime',
    'checkServerName':               'Server Config & Runtime',

    # Server List & Accounts
    'sortBrowserList':               'Server List & Accounts',
    'createRegionFile':              'Server List & Accounts',
    'getMapDirectoryTable':          'Server List & Accounts',
    'deleteSave':                    'Server List & Accounts',
    'sendPlayerExtraInfo':           'Server List & Accounts',
    'getServerAddressFromArgs':      'Server List & Accounts',
    'getServerPasswordFromArgs':     'Server List & Accounts',
    'getServerListFile':             'Server List & Accounts',
    'addServerToAccountList':        'Server List & Accounts',
    'updateServerToAccountList':     'Server List & Accounts',
    'deleteServerToAccountList':     'Server List & Accounts',
    'addAccountToAccountList':       'Server List & Accounts',
    'updateAccountToAccountList':    'Server List & Accounts',
    'deleteAccountToAccountList':    'Server List & Accounts',
    'getServerList':                 'Server List & Accounts',

    # Sound & Audio
    'getSLSoundManager':             'Sound & Audio',
    'getAmbientStreamManager':       'Sound & Audio',
    'playServerSound':               'Sound & Audio',
    'getWorldSoundManager':          'Sound & Audio',
    'AddWorldSound':                 'Sound & Audio',
    'AddNoiseToken':                 'Sound & Audio',
    'pauseSoundAndMusic':            'Sound & Audio',
    'resumeSoundAndMusic':           'Sound & Audio',
    'getBaseSoundBank':              'Sound & Audio',
    'getFMODSoundBank':              'Sound & Audio',
    'isSoundPlaying':                'Sound & Audio',
    'stopSound':                     'Sound & Audio',
    'getSoundManager':               'Sound & Audio',
    'testSound':                     'Sound & Audio',
    'getFMODEventPathList':          'Sound & Audio',
    'reloadSoundFiles':              'Sound & Audio',
    'addSound':                      'Sound & Audio',
    'sendPlaySound':                 'Sound & Audio',

    # Sprite Manager
    'getSpriteManager':              'Sprite Manager',
    'getSprite':                     'Sprite Manager',

    # Steam & Social
    'canInviteFriends':                      'Steam & Social',
    'inviteFriend':                          'Steam & Social',
    'getFriendsList':                        'Steam & Social',
    'getSteamModeActive':                    'Steam & Social',
    'getStreamModeActive':                   'Steam & Social',
    'getRemotePlayModeActive':               'Steam & Social',
    'isValidSteamID':                        'Steam & Social',
    'getCurrentUserSteamID':                 'Steam & Social',
    'getCurrentUserProfileName':             'Steam & Social',
    'getSteamScoreboard':                    'Steam & Social',
    'isSteamOverlayEnabled':                 'Steam & Social',
    'activateSteamOverlayToWorkshop':        'Steam & Social',
    'activateSteamOverlayToWorkshopUser':    'Steam & Social',
    'activateSteamOverlayToWorkshopItem':    'Steam & Social',
    'activateSteamOverlayToWebPage':         'Steam & Social',
    'getSteamProfileNameFromSteamID':        'Steam & Social',
    'getSteamAvatarFromSteamID':             'Steam & Social',
    'getSteamIDFromUsername':                'Steam & Social',
    'resetRegionFile':                       'Steam & Social',
    'getSteamProfileNameFromUsername':       'Steam & Social',
    'getSteamAvatarFromUsername':            'Steam & Social',
    'getSteamWorkshopStagedItems':           'Steam & Social',
    'getSteamWorkshopItemIDs':               'Steam & Social',
    'isSteamRunningOnSteamDeck':             'Steam & Social',
    'showSteamGamepadTextInput':             'Steam & Social',
    'showSteamFloatingGamepadTextInput':     'Steam & Social',
    'isFloatingGamepadTextInputVisible':     'Steam & Social',
    'querySteamWorkshopItemDetails':         'Steam & Social',

    # Steam Server Browser
    'connectToServerStateCallback':      'Steam Server Browser',
    'getPublicServersList':              'Steam Server Browser',
    'steamRequestInternetServersList':   'Steam Server Browser',
    'steamReleaseInternetServersRequest':'Steam Server Browser',
    'steamRequestInternetServersCount':  'Steam Server Browser',
    'steamGetInternetServerDetails':     'Steam Server Browser',
    'steamRequestServerRules':           'Steam Server Browser',
    'getHostByName':                     'Steam Server Browser',
    'steamRequestServerDetails':         'Steam Server Browser',
    'isPublicServerListAllowed':         'Steam Server Browser',
    'isSteamServerBrowserEnabled':       'Steam Server Browser',

    # String Utilities
    'checkStringPattern':            'String Utilities',
    'getTwoLetters':                 'String Utilities',
    'splitString':                   'String Utilities',

    # System Info
    'getGameVersion':                'System Info',
    'getBreakModGameVersion':        'System Info',
    'isSystemLinux':                 'System Info',
    'isSystemMacOS':                 'System Info',
    'isSystemWindows':               'System Info',

    # Text & Localization
    'getVideo':                      'Text & Localization',
    'getTextManager':                'Text & Localization',
    'setProgressBarValue':           'Text & Localization',
    'getText':                       'Text & Localization',
    'getTextOrNull':                 'Text & Localization',
    'getItemText':                   'Text & Localization',
    'getRadioText':                  'Text & Localization',
    'getTextMediaEN':                'Text & Localization',

    # Textures
    'useTextureFiltering':           'Textures',
    'getTexture':                    'Textures',
    'tryGetTexture':                 'Textures',

    # Timers
    'timersShowMean':                'Timers',
    'timersShowTotal':               'Timers',
    'timersReset':                   'Timers',
    'timerGetKept':                  'Timers',

    # Timestamps
    'getTimestamp':                  'Timestamps',
    'getTimestampMs':                'Timestamps',
    'getGametimeTimestamp':          'Timestamps',

    # Trading
    'requestTrading':                'Trading',
    'acceptTrading':                 'Trading',
    'tradingUISendAddItem':          'Trading',
    'tradingUISendRemoveItem':       'Trading',
    'tradingUISendUpdateState':      'Trading',
    'sendWarManagerUpdate':          'Trading',

    # User & Role Management
    'requestUsers':                  'User & Role Management',
    'requestPVPEvents':              'User & Role Management',
    'clearPVPEvents':                'User & Role Management',
    'getUsers':                      'User & Role Management',
    'networkUserAction':             'User & Role Management',
    'banUnbanUserAction':            'User & Role Management',
    'teleportUserAction':            'User & Role Management',
    'teleportToHimUserAction':       'User & Role Management',
    'requestRoles':                  'User & Role Management',
    'getRoles':                      'User & Role Management',
    'getCapabilities':               'User & Role Management',
    'addRole':                       'User & Role Management',
    'setupRole':                     'User & Role Management',
    'deleteRole':                    'User & Role Management',
    'setDefaultRoleFor':             'User & Role Management',
    'moveRole':                      'User & Role Management',

    # User Logs
    'scoreboardUpdate':              'User Logs',
    'requestUserlog':                'User Logs',
    'addUserlog':                    'User Logs',
    'removeUserlog':                 'User Logs',

    # Vehicle Management
    'sendSwitchSeat':                'Vehicle Management',
    'getVehicleById':                'Vehicle Management',
    'removeVehicle':                 'Vehicle Management',
    'removeAllVehicles':             'Vehicle Management',
    'addVehicle':                    'Vehicle Management',
    'addVehicleDebug':               'Vehicle Management',
    'attachTrailerToPlayerVehicle':  'Vehicle Management',

    # Vehicle Placement
    'addAllVehicles':                'Vehicle Placement',
    'addAllBurntVehicles':           'Vehicle Placement',
    'addAllSmashedVehicles':         'Vehicle Placement',
    'addPhysicsObject':              'Vehicle Placement',

    # World & Map Dimensions
    'createTile':                    'World & Map Dimensions',
    'getSquare':                     'World & Map Dimensions',
    'getWorld':                      'World & Map Dimensions',
    'getCell':                       'World & Map Dimensions',
    'getCellSizeInChunks':           'World & Map Dimensions',
    'getCellSizeInSquares':          'World & Map Dimensions',
    'getChunkSizeInSquares':         'World & Map Dimensions',
    'getMinimumWorldLevel':          'World & Map Dimensions',
    'getMaximumWorldLevel':          'World & Map Dimensions',
    'getCellMinX':                   'World & Map Dimensions',
    'getCellMaxX':                   'World & Map Dimensions',
    'getCellMinY':                   'World & Map Dimensions',
    'getCellMaxY':                   'World & Map Dimensions',

    # World Effects
    'addBloodSplat':                 'World Effects',
    'addCarCrash':                   'World Effects',
    'createRandomDeadBody':          'World Effects',

    # World Info Queries
    'getZombieInfo':                 'World Info Queries',
    'getPlayerInfo':                 'World Info Queries',
    'getMapInfo':                    'World Info Queries',
    'getVehicleInfo':                'World Info Queries',
    'getLotDirectories':             'World Info Queries',

    # XP & Player Stats
    'sendIconFound':                 'XP & Player Stats',
    'getLoosingXpValue':             'XP & Player Stats',
    'getLoosingXpTick':              'XP & Player Stats',
    'addXpNoMultiplier':             'XP & Player Stats',
    'addXp':                         'XP & Player Stats',
    'addXpMultiplier':               'XP & Player Stats',
    'syncBodyPart':                  'XP & Player Stats',
    'syncPlayerStats':               'XP & Player Stats',
    'sendPlayerStat':                'XP & Player Stats',
    'sendPlayerNutrition':           'XP & Player Stats',
    'SyncXp':                        'XP & Player Stats',

    # Zombie & Horde Spawning
    'createHordeFromTo':             'Zombie & Horde Spawning',
    'createHordeInAreaTo':           'Zombie & Horde Spawning',
    'spawnHorde':                    'Zombie & Horde Spawning',
    'createZombie':                  'Zombie & Horde Spawning',
    'addZombieSitting':              'Zombie & Horde Spawning',
    'addZombiesEating':              'Zombie & Horde Spawning',
    'addZombiesInOutfitArea':        'Zombie & Horde Spawning',
    'addZombiesInOutfit':            'Zombie & Horde Spawning',
    'addZombiesInBuilding':          'Zombie & Horde Spawning',

    # Zone Queries
    'getZone':                       'Zone Queries',
    'getZones':                      'Zone Queries',
    'getVehicleZoneAt':              'Zone Queries',
}

# Mid-level sections and top-level domains derived from group.
# Structure: group → (domain, section)
HIERARCHY = {
    # ── World ─────────────────────────────────────────────────────────────
    'World & Map Dimensions':   ('World', 'Map & Navigation'),
    'Zone Queries':             ('World', 'Map & Navigation'),
    'Coordinate Conversion':    ('World', 'Map & Navigation'),
    'Overhead Map':             ('World', 'Map & Navigation'),
    'Timestamps':               ('World', 'Map & Navigation'),

    'Environment & Climate':    ('World', 'Environment'),
    'World Effects':            ('World', 'Environment'),
    'World Info Queries':       ('World', 'Environment'),

    'Animal Spawning':          ('World', 'Spawning'),
    'Animal Hutch & Transport': ('World', 'Spawning'),
    'Zombie & Horde Spawning':  ('World', 'Spawning'),
    'Vehicle Management':       ('World', 'Spawning'),
    'Vehicle Placement':        ('World', 'Spawning'),

    # ── Player ────────────────────────────────────────────────────────────
    'Player Management':        ('Player', 'Player State'),
    'Player Data Sync':         ('Player', 'Player State'),
    'Player Database':          ('Player', 'Player State'),
    'XP & Player Stats':        ('Player', 'Player State'),

    'Character Customization':  ('Player', 'Character'),
    'Game Actions':             ('Player', 'Character'),
    'Hit & Combat Events':      ('Player', 'Character'),

    # ── Networking ────────────────────────────────────────────────────────
    'Multiplayer Connection':   ('Networking', 'Connection'),
    'Server / Client State':    ('Networking', 'Connection'),
    'Client/Server Commands':   ('Networking', 'Connection'),
    'Game Client':              ('Networking', 'Connection'),

    'Server Config & Runtime':  ('Networking', 'Server Admin'),
    'Server List & Accounts':   ('Networking', 'Server Admin'),
    'Admin: Bans & Tickets':    ('Networking', 'Server Admin'),
    'User & Role Management':   ('Networking', 'Server Admin'),

    'Factions & Safehouses':    ('Networking', 'Social'),
    'Chat Processing':          ('Networking', 'Social'),
    'Radio & Broadcast':        ('Networking', 'Social'),
    'Trading':                  ('Networking', 'Social'),

    'Steam & Social':           ('Networking', 'Steam'),
    'Steam Server Browser':     ('Networking', 'Steam'),

    # ── Input & Rendering ─────────────────────────────────────────────────
    'Keyboard Input':           ('Input & Rendering', 'Input Devices'),
    'Key Events':               ('Input & Rendering', 'Input Devices'),
    'Gamepad Input':            ('Input & Rendering', 'Input Devices'),
    'Mouse Input':              ('Input & Rendering', 'Input Devices'),
    'Controller Settings':      ('Input & Rendering', 'Input Devices'),

    'Rendering & Performance':  ('Input & Rendering', 'Rendering'),
    '3D Model Loading':         ('Input & Rendering', 'Rendering'),
    'Textures':                 ('Input & Rendering', 'Rendering'),
    'Sprite Manager':           ('Input & Rendering', 'Rendering'),

    'Sound & Audio':            ('Input & Rendering', 'Audio'),

    'Text & Localization':      ('Input & Rendering', 'Text'),

    # ── Game Systems ──────────────────────────────────────────────────────
    'Item Information':         ('Game Systems', 'Items'),
    'Item Scripting':           ('Game Systems', 'Items'),
    'Item Sync':                ('Game Systems', 'Items'),
    'Item Transactions & Actions': ('Game Systems', 'Items'),
    'Container Operations':     ('Game Systems', 'Items'),
    'Inventory Management':     ('Game Systems', 'Items'),

    'Game Configuration':       ('Game Systems', 'Configuration'),
    'Game Speed & Pause':       ('Game Systems', 'Configuration'),
    'Event System':             ('Game Systems', 'Configuration'),
    'System Info':              ('Game Systems', 'Configuration'),

    'Game Saves':               ('Game Systems', 'Saves & Logs'),
    'Save Info':                ('Game Systems', 'Saves & Logs'),
    'Save Management':          ('Game Systems', 'Saves & Logs'),
    'User Logs':                ('Game Systems', 'Saves & Logs'),

    'File I/O':                 ('Game Systems', 'Files & Mods'),
    'File System':              ('Game Systems', 'Files & Mods'),
    'Lua File I/O':             ('Game Systems', 'Files & Mods'),
    'Mod Management':           ('Game Systems', 'Files & Mods'),
    'Mod Utilities':            ('Game Systems', 'Files & Mods'),

    # ── Scripting & Debug ─────────────────────────────────────────────────
    'Debug Tools':              ('Scripting & Debug', 'Debug'),
    'Debugger & Lua Inspector': ('Scripting & Debug', 'Debug'),
    'Debug Editor Views':       ('Scripting & Debug', 'Debug'),
    'Debug Rendering':          ('Scripting & Debug', 'Debug'),

    'Entity & Script Reload':   ('Scripting & Debug', 'Lua'),
    'Lua Runtime':              ('Scripting & Debug', 'Lua'),
    'Lua Stack Introspection':  ('Scripting & Debug', 'Lua'),

    'Java Reflection':          ('Scripting & Debug', 'Utilities'),
    'String Utilities':         ('Scripting & Debug', 'Utilities'),
    'Timers':                   ('Scripting & Debug', 'Utilities'),
    'Miscellaneous Utilities':  ('Scripting & Debug', 'Utilities'),
    'Authentication':           ('Scripting & Debug', 'Utilities'),
}

api = json.loads(API_FILE.read_text(encoding="utf-8"))
fns = api["global_functions"]
print(f"Patching {len(fns)} global functions...")

ungrouped = []
no_hierarchy = []
for fn in fns:
    grp = FUNCTION_GROUPS.get(fn["lua_name"])
    if grp:
        fn["group"] = grp
    else:
        fn["group"] = "Other"
        ungrouped.append(fn["lua_name"])

    h = HIERARCHY.get(fn["group"])
    if h:
        fn["domain"], fn["section"] = h
    else:
        fn["domain"] = "Other"
        fn["section"] = "Other"
        if fn["group"] != "Other":
            no_hierarchy.append(fn["group"])

if ungrouped:
    print(f"WARNING: {len(ungrouped)} functions not in FUNCTION_GROUPS (add them manually):")
    for name in ungrouped:
        print(f"  {name!r}")
elif no_hierarchy:
    missing = sorted(set(no_hierarchy))
    print(f"WARNING: {len(missing)} groups missing from HIERARCHY: {missing}")
else:
    groups_used = sorted(set(f["group"] for f in fns))
    sections_used = sorted(set(f["section"] for f in fns))
    domains_used = sorted(set(f["domain"] for f in fns))
    print(f"OK: {len(domains_used)} domains / {len(sections_used)} sections / {len(groups_used)} groups")

API_FILE.write_text(json.dumps(api, indent=2), encoding="utf-8")
print("Done.")

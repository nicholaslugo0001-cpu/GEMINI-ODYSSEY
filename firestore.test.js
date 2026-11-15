const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProgress(uid) {
  return {
    userId: uid,
    displayName: "Gemini Knight",
    googleCoins: 100,
    totalCoinsCollected: 250,
    level: 2,
    xp: 35,
    skillPoints: 1,
    attackStat: 2,
    defenseStat: 1,
    staminaStat: 1,
    healthStat: 1,
    tpuCoreLevel: 1,
    contextWindowLevel: 1,
    fiberOpticLevel: 1,
    neuralMagnetLevel: 1,
    sectorStars: { search_plains: 4 },
    missionsCompleted: 1,
    virusesPurged: 5,
    adwareBlocked: 2,
    hackersOutsmarted: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validLeaderboard(uid) {
  return {
    userId: uid,
    displayName: "Gemini Knight",
    level: 2,
    totalStars: 4,
    totalCoinsCollected: 250,
    missionsCompleted: 1,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write users or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProgress(ALICE_UID)));
});

test("Authenticated user can create and read own progress, cannot read or write Bob's progress", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProgress(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set(validProgress(ALICE_UID)));
});

test("Shadow update with ghost field is rejected on users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProgress(ALICE_UID),
      isAdmin: true,
    })
  );
});

test("Authenticated user can write own leaderboard entry and list global leaderboard, cannot overwrite Bob's entry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboard(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("leaderboard").get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("leaderboard").get());
  await assertFails(bobDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboard(ALICE_UID)));
});

function validRoom(roomId, hostUid) {
  return {
    roomId,
    roomCode: roomId,
    hostUid,
    sectorId: "sector_plaza",
    status: "waiting",
    memberUids: [hostUid],
    wave: 1,
    nodesIndexed: 0,
    bossHp: 0,
    bossMaxHp: 0,
    enemySnapshots: [],
    isVictory: false,
    starsEarned: 0,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validRoomPlayer(roomId, uid, colorIndex) {
  return {
    userId: uid,
    roomId,
    displayName: `Knight ${uid}`,
    colorIndex,
    ready: false,
    x: 0,
    y: 0,
    facingAngle: -1.57,
    hp: 130,
    maxHp: 130,
    isGuarding: false,
    isSlashing: false,
    slashComboStep: 0,
    walkPhase: 0,
    hitSeq: 0,
    lastHitEnemyId: -1,
    lastHitDamage: 0,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

test("Co-op room supports up to 4 players, blocks 5th player, and isolates player writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const daveDb = testEnv.authenticatedContext("dave_000").firestore();
  const eveDb = testEnv.authenticatedContext("eve_999").firestore();

  const roomId = "GM4X9K";
  await assertSucceeds(aliceDb.collection("rooms").doc(roomId).set(validRoom(roomId, ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).collection("players").doc(ALICE_UID).set(validRoomPlayer(roomId, ALICE_UID, 0))
  );

  // Bob, Charlie, Dave join (total 4)
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    bobDb.collection("rooms").doc(roomId).collection("players").doc(BOB_UID).set(validRoomPlayer(roomId, BOB_UID, 1))
  );
  await assertSucceeds(
    charlieDb.collection("rooms").doc(roomId).update({
      memberUids: [ALICE_UID, BOB_UID, "charlie_789"],
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    daveDb.collection("rooms").doc(roomId).update({
      memberUids: [ALICE_UID, BOB_UID, "charlie_789", "dave_000"],
      updatedAt: new Date(),
    })
  );

  // 5th player Eve must fail
  await assertFails(
    eveDb.collection("rooms").doc(roomId).update({
      memberUids: [ALICE_UID, BOB_UID, "charlie_789", "dave_000", "eve_999"],
      updatedAt: new Date(),
    })
  );

  // Non-host Bob cannot start/change stage status, and cannot overwrite Alice's player doc
  await assertFails(
    bobDb.collection("rooms").doc(roomId).update({ status: "playing", updatedAt: new Date() })
  );
  await assertFails(
    bobDb.collection("rooms").doc(roomId).collection("players").doc(ALICE_UID).update({ hp: 1, updatedAt: new Date() })
  );

  // Host Alice starts and finishes room; terminal finished state rejects subsequent edits
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).update({ status: "playing", updatedAt: new Date() })
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc(roomId).update({
      status: "finished",
      isVictory: true,
      starsEarned: 5,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("rooms").doc(roomId).update({ status: "waiting", updatedAt: new Date() })
  );
});

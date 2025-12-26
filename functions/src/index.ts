import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

/**
 * SETUP AWAL
 * User yang login boleh set role UNTUK DIRINYA SENDIRI
 * (nanti bisa dikunci)
 */
export const setMyRole = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Login dulu"
      );
    }

    const role = data?.role;
    const uid = context.auth.uid;

    if (!role) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "role required"
      );
    }

    if (!["member", "admin", "super_admin"].includes(role)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "invalid role"
      );
    }

    await admin.auth().setCustomUserClaims(uid, { role });

    return { ok: true, uid, role };
  }
);

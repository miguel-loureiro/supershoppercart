package com.supershopcart.utils;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public class RefreshTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    final Firestore firestore; // Declare Firestore to be injected

    /**
     * Constructor injection for the Firestore instance.
     * Spring will automatically provide the 'Firestore' bean configured in FirebaseConfig.
     *
     * @param firestore The Firestore client instance.
     */
    @Autowired // Mark constructor for Spring's autowiring
    public RefreshTokenCleanupJob(Firestore firestore) {
        this.firestore = firestore; // Assign the injected Firestore instance
    }

    /**
     * Run every night at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // 2:00 AM every day
    public void deleteExpiredRefreshTokens() throws Exception {
        long now = System.currentTimeMillis();

        logger.info("🧹 Starting cleanup of expired refresh tokens...");

        // Use the injected 'firestore' instance
        ApiFuture<QuerySnapshot> future = firestore.collection("refresh_tokens")
                .whereLessThan("expiry", now)
                .get();

        List<QueryDocumentSnapshot> expiredTokens = future.get().getDocuments();

        for (QueryDocumentSnapshot doc : expiredTokens) { // Use QueryDocumentSnapshot for iteration
            doc.getReference().delete();
            logger.info("Deleted expired token: {}", doc.getId());
        }

        logger.info("✅ Expired token cleanup completed. Total deleted: {}", expiredTokens.size());
    }
}

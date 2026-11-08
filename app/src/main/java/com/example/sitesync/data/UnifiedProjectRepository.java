package com.example.sitesync.data;

import android.content.Context;

/**
 * UnifiedProjectRepository located in com.example.sitesync.data package.
 */
public class UnifiedProjectRepository extends com.example.sitesync.data.repository.UnifiedProjectRepository {

    public UnifiedProjectRepository(Context context) {
        super(context);
    }

    public static synchronized UnifiedProjectRepository getInstance(Context context) {
        return new UnifiedProjectRepository(context);
    }
}

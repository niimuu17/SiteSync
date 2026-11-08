package com.example.sitesync.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.data.DocumentRepository;
import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.DailySiteReport;
import com.example.sitesync.models.SiteDocument;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.models.TechnicalIssue;

import java.util.List;

/**
 * Unified Project Repository Facade.
 * Consolidates all jobsite telemetry, progress, quality control, documents,
 * and daily logs into a single centralized entry point for the Unified Site Cockpit.
 */
public class UnifiedProjectRepository {

    private static volatile UnifiedProjectRepository instance;

    private final ProgressRepository progressRepository;
    private final InspectionRepository inspectionRepository;
    private final DocumentRepository documentRepository;
    private final IssueRepository issueRepository;
    private final DailyReportRepository dailyReportRepository;

    public UnifiedProjectRepository(Context context) {
        Context appCtx = context.getApplicationContext();
        this.progressRepository = new ProgressRepository(appCtx);
        this.inspectionRepository = new InspectionRepository(appCtx);
        this.documentRepository = new DocumentRepository(appCtx);
        this.issueRepository = new IssueRepository(appCtx);
        this.dailyReportRepository = new DailyReportRepository(appCtx);
    }

    public static synchronized UnifiedProjectRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UnifiedProjectRepository(context);
        }
        return instance;
    }

    public ProgressRepository getProgressRepository() {
        return progressRepository;
    }

    public InspectionRepository getInspectionRepository() {
        return inspectionRepository;
    }

    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    public IssueRepository getIssueRepository() {
        return issueRepository;
    }

    public DailyReportRepository getDailyReportRepository() {
        return dailyReportRepository;
    }

    // Direct LiveData accessors for cockpit reactive UI
    public LiveData<List<ConstructionActivity>> getAllActivities() {
        return progressRepository.getAllActivitiesLive();
    }

    public LiveData<List<SiteInspection>> getAllInspections() {
        return inspectionRepository.getAllInspectionsLive();
    }

    public LiveData<List<SiteDocument>> getAllDocuments() {
        return documentRepository.getAllDocumentsLive();
    }

    public LiveData<List<TechnicalIssue>> getAllIssues() {
        return issueRepository.getAllIssuesLive();
    }

    public LiveData<List<DailySiteReport>> getAllReports() {
        return dailyReportRepository.getAllReportsLive();
    }

    public int getOverallProgressPercentage() {
        return progressRepository.calculateOverallProgress();
    }
}

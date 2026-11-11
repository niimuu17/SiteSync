package com.example.sitesync.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.ProjectDao;
import com.example.sitesync.data.local.entities.ProjectEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProjectRepository {

    private static volatile ProjectRepository instance;
    private final ProjectDao projectDao;
    private final ExecutorService executor;

    public interface ActionCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    private ProjectRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.projectDao = db.projectDao();
        this.executor = Executors.newFixedThreadPool(2);
    }

    public static synchronized ProjectRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ProjectRepository(context);
        }
        return instance;
    }

    public LiveData<List<ProjectEntity>> getAllProjectsLive() {
        return projectDao.getAllProjectsLive();
    }

    public LiveData<List<ProjectEntity>> getRecentProjectsLive(int limit) {
        return projectDao.getRecentProjectsLive(limit);
    }

    public LiveData<ProjectEntity> getProjectByIdLive(String id) {
        return projectDao.getProjectByIdLive(id);
    }

    public LiveData<List<ProjectEntity>> getProjectsByCodesLive(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            androidx.lifecycle.MutableLiveData<List<ProjectEntity>> empty = new androidx.lifecycle.MutableLiveData<>();
            empty.setValue(new ArrayList<>());
            return empty;
        }
        return projectDao.getProjectsByCodesLive(codes);
    }

    public void getProjectById(String id, ActionCallback<ProjectEntity> callback) {
        executor.execute(() -> {
            try {
                ProjectEntity project = projectDao.getProjectById(id);
                if (callback != null) callback.onSuccess(project);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void saveProject(ProjectEntity project, ActionCallback<Void> callback) {
        executor.execute(() -> {
            try {
                projectDao.insert(project);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void deleteProject(String projectId, ActionCallback<Void> callback) {
        executor.execute(() -> {
            try {
                projectDao.deleteById(projectId);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void recordProjectOpened(String projectId) {
        executor.execute(() -> {
            try {
                projectDao.updateLastOpened(projectId, System.currentTimeMillis());
            } catch (Exception ignored) {}
        });
    }

    public void updateProgressMetrics(String projectId, float overall, float civil, float electric,
                                       float plumbing, float thaiGlass, float tiles, float interior,
                                       ActionCallback<Void> callback) {
        executor.execute(() -> {
            try {
                projectDao.updateProgressMetrics(projectId, overall, civil, electric, plumbing, thaiGlass, tiles, interior);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void seedDefaultProjectsIfEmpty(ActionCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                int count = projectDao.getProjectCount();
                if (count > 0) {
                    if (callback != null) callback.onSuccess(false);
                    return;
                }

                List<ProjectEntity> seeds = new ArrayList<>();
                long now = System.currentTimeMillis();

                // 1. Metro Central Tower (Primary flagship)
                ProjectEntity p1 = new ProjectEntity(
                        "PRJ-MCT-2026",
                        "MCT-2026",
                        "Metro Central Tower",
                        "Metro Urban Development Corp.",
                        "Skyline Infrastructure Ltd.",
                        "Dhaka Central (23.8103° N, 90.4125° E)",
                        "৳ 42.5 Crore",
                        "Level 3 (Perimeter Columns)",
                        "Commercial Multi-Storey (14 Floors)",
                        28,
                        11.5f,
                        56,
                        32,
                        "Porcelain Floor Tiles (24x24)",
                        "{\"spec\":\"14-Story RCC Commercial Tower with 2 basement parkings. 11.5ft floor-to-floor height, heavy Thai aluminum facade, central HVAC and 3 elevator shafts.\"}",
                        68.0f
                );
                p1.setLastOpenedAt(now);
                p1.setCivilProgressPercent(92f);
                p1.setElectricProgressPercent(65f);
                p1.setPlumbingProgressPercent(58f);
                p1.setThaiGlassProgressPercent(45f);
                p1.setTilesFlooringProgressPercent(30f);
                p1.setInteriorPlasterProgressPercent(50f);
                p1.setExteriorRoofProgressPercent(40f);
                seeds.add(p1);

                // 2. Uttara Lakeview Villa
                ProjectEntity p2 = new ProjectEntity(
                        "PRJ-ULV-2026",
                        "ULV-2026",
                        "Uttara Lakeview Villa",
                        "Engr. Tariqul Islam",
                        "Bengal Construction Ltd.",
                        "Sector 7, Uttara, Dhaka",
                        "৳ 1.2 Crore",
                        "Ground Floor Masonry",
                        "2-Room Luxury Villa",
                        2,
                        10.0f,
                        6,
                        4,
                        "Vitrified Tiles (24x24)",
                        "{\"spec\":\"2-Room residential duplex building with 10ft high interior brick walls, 6 Thai sliding windows, 4 wooden doors, and vitrified floor tiles.\"}",
                        42.0f
                );
                p2.setLastOpenedAt(now - 3600000L);
                p2.setCivilProgressPercent(75f);
                p2.setElectricProgressPercent(35f);
                p2.setPlumbingProgressPercent(30f);
                p2.setThaiGlassProgressPercent(20f);
                p2.setTilesFlooringProgressPercent(15f);
                p2.setInteriorPlasterProgressPercent(40f);
                p2.setExteriorRoofProgressPercent(25f);
                seeds.add(p2);

                // 3. Dhanmondi Heights
                ProjectEntity p3 = new ProjectEntity(
                        "PRJ-DHN-2026",
                        "DHN-2026",
                        "Dhanmondi Heights",
                        "Imperial Properties Ltd.",
                        "Padma Builders",
                        "Road 27, Dhanmondi, Dhaka",
                        "৳ 8.5 Crore",
                        "Level 6 Slab Pouring",
                        "8-Floor Residential Apartment",
                        16,
                        10.5f,
                        32,
                        20,
                        "Mosaic & Ceramic Tiles",
                        "{\"spec\":\"8-Floor modern apartment building with 10.5ft ceiling height, concealed conduit electrical wiring, CPVC sanitary pipe fittings, and mosaic utility flooring.\"}",
                        55.0f
                );
                p3.setLastOpenedAt(now - 7200000L);
                p3.setCivilProgressPercent(80f);
                p3.setElectricProgressPercent(50f);
                p3.setPlumbingProgressPercent(45f);
                p3.setThaiGlassProgressPercent(30f);
                p3.setTilesFlooringProgressPercent(25f);
                p3.setInteriorPlasterProgressPercent(40f);
                p3.setExteriorRoofProgressPercent(35f);
                seeds.add(p3);

                // 4. Mirpur Civic Center
                ProjectEntity p4 = new ProjectEntity(
                        "PRJ-MCC-2026",
                        "MCC-2026",
                        "Mirpur Civic Center",
                        "DNCC Infrastructure Dept.",
                        "National Construction Co.",
                        "Mirpur 10 Circle, Dhaka",
                        "৳ 15.0 Crore",
                        "Level 2 Finishing",
                        "Public Civic Center (4 Floors)",
                        12,
                        12.0f,
                        24,
                        16,
                        "Heavy-Duty Polished Mosaic",
                        "{\"spec\":\"4-Story community civic center with high ceiling multipurpose auditoriums, 12ft walls, polished mosaic flooring, and fire sprinkler plumbing.\"}",
                        82.0f
                );
                p4.setLastOpenedAt(now - 14400000L);
                p4.setCivilProgressPercent(98f);
                p4.setElectricProgressPercent(85f);
                p4.setPlumbingProgressPercent(80f);
                p4.setThaiGlassProgressPercent(75f);
                p4.setTilesFlooringProgressPercent(70f);
                p4.setInteriorPlasterProgressPercent(85f);
                p4.setExteriorRoofProgressPercent(80f);
                seeds.add(p4);

                // 5. Gulshan Avenue Residency
                ProjectEntity p5 = new ProjectEntity(
                        "PRJ-GAR-2026",
                        "GAR-2026",
                        "Gulshan Avenue Residency",
                        "Apex Real Estate Group",
                        "Eastern Housing Alliance",
                        "Gulshan 2, Dhaka",
                        "৳ 22.0 Crore",
                        "Basement Foundation Curing",
                        "Luxury Condominium Complex",
                        40,
                        11.0f,
                        80,
                        50,
                        "Italian Marble & Porcelain Tiles",
                        "{\"spec\":\"Twin-tower luxury condominium with 11ft wall heights, premium double-glazed Thai glass windows, Italian marble flooring, and smart home electrical conduits.\"}",
                        24.0f
                );
                p5.setLastOpenedAt(now - 28800000L);
                p5.setCivilProgressPercent(45f);
                p5.setElectricProgressPercent(15f);
                p5.setPlumbingProgressPercent(10f);
                p5.setThaiGlassProgressPercent(5f);
                p5.setTilesFlooringProgressPercent(0f);
                p5.setInteriorPlasterProgressPercent(10f);
                p5.setExteriorRoofProgressPercent(0f);
                seeds.add(p5);

                projectDao.insertAll(seeds);
                if (callback != null) callback.onSuccess(true);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }
}

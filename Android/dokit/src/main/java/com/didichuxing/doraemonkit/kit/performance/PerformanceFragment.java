package com.didichuxing.doraemonkit.kit.performance;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.didichuxing.doraemonkit.R;
import com.didichuxing.doraemonkit.constant.BundleKey;
import com.didichuxing.doraemonkit.kit.core.BaseFragment;
import com.didichuxing.doraemonkit.widget.titlebar.TitleBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PerformanceFragment extends BaseFragment {
    public final static int CPU = 0;
    public final static int RAM = 1;
    public final static int FPS = 2;

    private PerformanceDataAdapter performanceDataAdapter;
    private PolyLineAdapter adapter;
    private TextView parameter;
    private TextView time;
    private TextView date;
    /**
     * AsyncTask was removed from the modern Android API surface.  Keep file I/O
     * off the UI thread with a short-lived, fragment-owned executor instead.
     */
    private final ExecutorService dataExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Future<?> loadDataFuture;
    private volatile boolean viewActive;

    @Override

    protected int onRequestLayout() {
        return R.layout.dk_fragment_cpu_cache_log;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PerformanceDataManager.getInstance().init();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewActive = true;
        initview();
    }

    private void initview() {


        TitleBar titleBar = findViewById(R.id.title_bar);
        titleBar.setOnTitleBarClickListener(new TitleBar.OnTitleBarClickListener() {
            @Override
            public void onLeftClick() {
                getActivity().onBackPressed();
            }

            @Override
            public void onRightClick() {

            }
        });
        RecyclerView dataShow = findViewById(R.id.data_show);

        PolyLineAdapter.Builder builder = new PolyLineAdapter.Builder(getActivity(), 10);

        dataShow.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));


        RecyclerView detail = findViewById(R.id.data_detail);
        detail.setLayoutManager(new LinearLayoutManager(getActivity()));
        performanceDataAdapter = new PerformanceDataAdapter(getActivity());
        detail.setAdapter(performanceDataAdapter);


        TextView model = findViewById(R.id.model);
        String dataPath = null;
        Bundle arguments = getArguments();
        if (arguments != null) {
            int type = arguments.getInt(BundleKey.PERFORMANCE_TYPE, CPU);
            if (type == RAM) {
                builder.setMaxValue((int) PerformanceDataManager.getInstance().getMaxMemory()).setMinValue(0);
                model.setText(R.string.dk_frameinfo_ram);
                dataPath = PerformanceDataManager.getInstance().getMemoryFilePath();
            } else if (type == FPS) {
                builder.setMaxValue(100).setMinValue(0);
                model.setText(R.string.dk_frameinfo_fps);
                dataPath = PerformanceDataManager.getInstance().getFpsFilePath();
            } else {
                builder.setMaxValue(100).setMinValue(0);
                model.setText(R.string.dk_frameinfo_cpu);
                dataPath = PerformanceDataManager.getInstance().getCpuFilePath();
            }
        }

        adapter = builder.build();
        dataShow.setAdapter(adapter);
        if (dataPath != null) {
            loadData(dataPath);
        }


        parameter = findViewById(R.id.parameter);
        time = findViewById(R.id.time);
        date = findViewById(R.id.date);

        performanceDataAdapter.setOnViewClickListener(new PerformanceDataAdapter.OnViewClickListener() {
            @Override
            public void onViewClick(View v, PerformanceData data) {
                updateTips(data);
            }
        });

        adapter.setOnViewClickListener(new PolyLineAdapter.OnViewClickListener() {
            @Override
            public void onViewClick(int position, PerformanceData data) {
                updateTips(data);
            }
        });


    }

    private void updateTips(PerformanceData data) {
        parameter.setText(String.valueOf(data.parameter));
        time.setText(data.time);
        date.setText(data.date);
    }


    private void loadData(final String path) {
        if (loadDataFuture != null) {
            loadDataFuture.cancel(true);
        }
        loadDataFuture = dataExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final List<PerformanceData> result = readData(path);
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!viewActive || performanceDataAdapter == null || adapter == null) {
                            return;
                        }
                        performanceDataAdapter.append(result);
                        adapter.setData(result);
                        if (result.size() > 1) {
                            updateTips(result.get(1));
                        }
                    }
                });
            }
        });
    }

    private List<PerformanceData> readData(String path) {
        File file = new File(path);
        ArrayList<PerformanceData> datas = new ArrayList<>();
        if (!file.exists()) {
            return datas;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String tempString;
            while (!Thread.currentThread().isInterrupted() && (tempString = reader.readLine()) != null) {
                String[] split = tempString.trim().split("\\s+");
                if (split.length >= 3) {
                    datas.add(new PerformanceData(split[1], split[2], Float.valueOf(split[0])));
                }
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                e.printStackTrace();
            }
        }
        return datas;
    }

    @Override
    public void onDestroyView() {
        viewActive = false;
        if (loadDataFuture != null) {
            loadDataFuture.cancel(true);
            loadDataFuture = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        dataExecutor.shutdownNow();
        super.onDestroy();
    }


}

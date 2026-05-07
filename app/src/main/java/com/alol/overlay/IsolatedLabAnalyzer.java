package com.alol.overlay;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IsolatedLabAnalyzer {
    private Context ctx;
    public IsolatedLabAnalyzer(Context c) { this.ctx = c; }

    public List<ExtractorActivity.OffsetItem> analyzeFile(String path) {
        List<ExtractorActivity.OffsetItem> results = new ArrayList<>();
        try {
            File f = new File(path);
            byte[] data = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(f);
            fis.read(data);
            fis.close();

            // تحليل ELF بسيط: البحث عن الرموز
            // هذا مثال مبسط - الواقع يتطلب تحليل ELF كامل
            String[] knownSymbols = {
                "AnoSDKInit", "AnoSDKGetReportData", "AnoSDKIoctl",
                "AnoSDKOnRecvData", "AnoSDKRegistInfoListener", "AnoSDKDelReportData"
            };

            for (String sym : knownSymbols) {
                byte[] pattern = sym.getBytes();
                int found = indexOf(data, pattern);
                if (found >= 0) {
                    String offset = "0x" + Integer.toHexString(found);
                    String threat = classifyThreat(sym);
                    results.add(new ExtractorActivity.OffsetItem(sym, offset, pattern.length, threat));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    private String classifyThreat(String symbol) {
        if (symbol.contains("Init") || symbol.contains("Ioctl")) return "🔥 CRITICAL";
        if (symbol.contains("Report") || symbol.contains("Recv")) return "⚠️ HIGH";
        if (symbol.contains("Del")) return "📋 MEDIUM";
        return "ℹ️ LOW";
    }

    private int indexOf(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) { match = false; break; }
            }
            if (match) return i;
        }
        return -1;
    }
}

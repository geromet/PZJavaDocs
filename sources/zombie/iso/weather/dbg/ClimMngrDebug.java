/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.dbg;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.SimplexNoise;
import zombie.iso.weather.WeatherPeriod;
import zombie.network.GameClient;

public class ClimMngrDebug
extends ClimateManager {
    private GregorianCalendar calendar;
    private double worldAgeHours;
    private double worldAgeHoursStart;
    private double weatherPeriodTime;
    private double simplexOffsetA;
    private final ClimateManager.AirFront currentFront;
    private final WeatherPeriod weatherPeriod;
    private boolean tickIsDayChange;
    public ArrayList<RunInfo> runs = new ArrayList();
    private RunInfo currentRun;
    private ErosionSeason season;
    private final int totalDaysPeriodIndexMod = 5;
    private boolean doOverrideSandboxRainMod;
    private int sandboxRainModOverride = 3;
    private int durDays;
    private static final int WEATHER_NORMAL = 0;
    private static final int WEATHER_STORM = 1;
    private static final int WEATHER_TROPICAL = 2;
    private static final int WEATHER_BLIZZARD = 3;
    private FileWriter writer;

    public ClimMngrDebug() {
        this.currentFront = new ClimateManager.AirFront();
        this.weatherPeriod = new WeatherPeriod(this, null);
        this.weatherPeriod.setPrintStuff(false);
    }

    public void setRainModOverride(int rainmod) {
        this.doOverrideSandboxRainMod = true;
        this.sandboxRainModOverride = rainmod;
    }

    public void unsetRainModOverride() {
        this.doOverrideSandboxRainMod = false;
        this.sandboxRainModOverride = 3;
    }

    public void SimulateDays(int amountOfDays, int totalRuns) {
        this.durDays = amountOfDays;
        DebugLog.log("Starting " + totalRuns + " simulations of " + amountOfDays + " days per run...");
        boolean startMonth = false;
        boolean startDay = false;
        DebugLog.log("Year: " + GameTime.instance.getYear() + ", Month: 0, Day: 0");
        for (int k = 0; k < totalRuns; ++k) {
            this.calendar = new GregorianCalendar(GameTime.instance.getYear(), 0, 0, 0, 0);
            this.season = ClimateManager.getInstance().getSeason().clone();
            this.season.init(this.season.getLat(), this.season.getTempMax(), this.season.getTempMin(), this.season.getTempDiff(), this.season.getSeasonLag(), this.season.getHighNoon(), Rand.Next(0, 255), Rand.Next(0, 255), Rand.Next(0, 255));
            this.simplexOffsetA = Rand.Next(0, 8000);
            this.weatherPeriodTime = this.worldAgeHours = 250.0;
            this.worldAgeHoursStart = this.worldAgeHours;
            double airMassNoiseFrequencyMod = this.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier());
            float airMass = (float)SimplexNoise.noise(this.simplexOffsetA, this.worldAgeHours / airMassNoiseFrequencyMod);
            int airType = airMass < 0.0f ? -1 : 1;
            this.currentFront.setFrontType(airType);
            this.weatherPeriod.stopWeatherPeriod();
            double nextDay = this.worldAgeHours + 24.0;
            int totalhours = amountOfDays * 24;
            this.currentRun = new RunInfo(this);
            this.currentRun.durationDays = amountOfDays;
            this.currentRun.durationHours = totalhours;
            this.currentRun.seedA = this.simplexOffsetA;
            this.runs.add(this.currentRun);
            for (int i = 0; i < totalhours; ++i) {
                this.tickIsDayChange = false;
                this.worldAgeHours += 1.0;
                if (this.worldAgeHours >= nextDay) {
                    this.tickIsDayChange = true;
                    nextDay += 24.0;
                    this.calendar.add(5, 1);
                    int day = this.calendar.get(5);
                    int month = this.calendar.get(2);
                    int year = this.calendar.get(1);
                    this.season.setDay(day, month, year);
                }
                this.update_sim();
            }
        }
        this.saveData();
    }

    private void update_sim() {
        int airType;
        double airMassNoiseFrequencyMod = this.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier());
        float airMass = (float)SimplexNoise.noise(this.simplexOffsetA, this.worldAgeHours / airMassNoiseFrequencyMod);
        int n = airType = airMass < 0.0f ? -1 : 1;
        if (this.currentFront.getType() != airType) {
            if (this.worldAgeHours > this.weatherPeriodTime) {
                this.weatherPeriod.initSimulationDebug(this.currentFront, this.worldAgeHours);
                this.recordAndCloseWeatherPeriod();
            }
            this.currentFront.setFrontType(airType);
        }
        if (!winterIsComing && !theDescendingFog && this.worldAgeHours >= this.worldAgeHoursStart + 72.0 && this.worldAgeHours <= this.worldAgeHoursStart + 96.0 && !this.weatherPeriod.isRunning() && this.worldAgeHours > this.weatherPeriodTime && Rand.Next(0, 1000) < 50) {
            this.triggerCustomWeatherStage(3, 10.0f);
        }
        if (this.tickIsDayChange) {
            double daymidpoint = Math.floor(this.worldAgeHours) + 12.0;
            float airMassDaily = (float)SimplexNoise.noise(this.simplexOffsetA, daymidpoint / airMassNoiseFrequencyMod);
            int n2 = airType = airMassDaily < 0.0f ? -1 : 1;
            if (airType == this.currentFront.getType()) {
                this.currentFront.addDaySample(airMassDaily);
            }
        }
    }

    private void recordAndCloseWeatherPeriod() {
        if (this.weatherPeriod.isRunning()) {
            if (this.worldAgeHours - this.weatherPeriodTime > 0.0) {
                this.currentRun.addRecord(this.worldAgeHours - this.weatherPeriodTime);
            }
            this.weatherPeriodTime = this.worldAgeHours + Math.ceil(this.weatherPeriod.getDuration());
            boolean storm = false;
            boolean tropical = false;
            boolean blizzard = false;
            for (WeatherPeriod.WeatherStage stage : this.weatherPeriod.getWeatherStages()) {
                if (stage.getStageID() == 3) {
                    storm = true;
                }
                if (stage.getStageID() == 8) {
                    tropical = true;
                }
                if (stage.getStageID() != 7) continue;
                blizzard = true;
            }
            this.currentRun.addRecord(this.currentFront.getType(), this.weatherPeriod.getDuration(), this.weatherPeriod.getFrontCache().getStrength(), storm, tropical, blizzard);
        }
        this.weatherPeriod.stopWeatherPeriod();
    }

    @Override
    public boolean triggerCustomWeatherStage(int stage, float duration) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            ClimateManager.AirFront front = new ClimateManager.AirFront();
            front.setFrontType(1);
            front.setStrength(0.95f);
            this.weatherPeriod.initSimulationDebug(front, this.worldAgeHours, stage, duration);
            this.recordAndCloseWeatherPeriod();
            return true;
        }
        return false;
    }

    @Override
    protected double getAirMassNoiseFrequencyMod(int sandboxRain) {
        if (this.doOverrideSandboxRainMod) {
            return super.getAirMassNoiseFrequencyMod(this.sandboxRainModOverride);
        }
        return super.getAirMassNoiseFrequencyMod(sandboxRain);
    }

    @Override
    protected float getRainTimeMultiplierMod(int sandboxRain) {
        if (this.doOverrideSandboxRainMod) {
            return super.getRainTimeMultiplierMod(this.sandboxRainModOverride);
        }
        return super.getRainTimeMultiplierMod(sandboxRain);
    }

    @Override
    public ErosionSeason getSeason() {
        return this.season;
    }

    @Override
    public float getDayMeanTemperature() {
        return this.season.getDayMeanTemperature();
    }

    @Override
    public void resetOverrides() {
    }

    private RunInfo calculateTotal() {
        RunInfo total = new RunInfo(this);
        total.totalDaysPeriod = new int[50];
        double totalDurPeriod = 0.0;
        double totalDurEmpty = 0.0;
        float totalStr = 0.0f;
        float totalWarmStr = 0.0f;
        float totalColdStr = 0.0f;
        for (RunInfo r : this.runs) {
            int i;
            if (r.totalPeriodDuration < total.mostDryPeriod) {
                total.mostDryPeriod = r.totalPeriodDuration;
            }
            if (r.totalPeriodDuration > total.mostWetPeriod) {
                total.mostWetPeriod = r.totalPeriodDuration;
            }
            total.totalPeriodDuration += r.totalPeriodDuration;
            if (r.longestPeriod > total.longestPeriod) {
                total.longestPeriod = r.longestPeriod;
            }
            if (r.shortestPeriod < total.shortestPeriod) {
                total.shortestPeriod = r.shortestPeriod;
            }
            total.totalPeriods += r.totalPeriods;
            total.averagePeriod += r.averagePeriod;
            if (r.longestEmpty > total.longestEmpty) {
                total.longestEmpty = r.longestEmpty;
            }
            if (r.shortestEmpty < total.shortestEmpty) {
                total.shortestEmpty = r.shortestEmpty;
            }
            total.totalEmpty += r.totalEmpty;
            total.averageEmpty += r.averageEmpty;
            if (r.highestStrength > total.highestStrength) {
                total.highestStrength = r.highestStrength;
            }
            if (r.lowestStrength < total.lowestStrength) {
                total.lowestStrength = r.lowestStrength;
            }
            total.averageStrength += r.averageStrength;
            if (r.highestWarmStrength > total.highestWarmStrength) {
                total.highestWarmStrength = r.highestWarmStrength;
            }
            if (r.lowestWarmStrength < total.lowestWarmStrength) {
                total.lowestWarmStrength = r.lowestWarmStrength;
            }
            total.averageWarmStrength += r.averageWarmStrength;
            if (r.highestColdStrength > total.highestColdStrength) {
                total.highestColdStrength = r.highestColdStrength;
            }
            if (r.lowestColdStrength < total.lowestColdStrength) {
                total.lowestColdStrength = r.lowestColdStrength;
            }
            total.averageColdStrength += r.averageColdStrength;
            total.countNormalWarm += r.countNormalWarm;
            total.countNormalCold += r.countNormalCold;
            total.countStorm += r.countStorm;
            total.countTropical += r.countTropical;
            total.countBlizzard += r.countBlizzard;
            for (i = 0; i < r.dayCountPeriod.length; ++i) {
                int n = i;
                total.dayCountPeriod[n] = total.dayCountPeriod[n] + r.dayCountPeriod[i];
            }
            for (i = 0; i < r.dayCountWarmPeriod.length; ++i) {
                int n = i;
                total.dayCountWarmPeriod[n] = total.dayCountWarmPeriod[n] + r.dayCountWarmPeriod[i];
            }
            for (i = 0; i < r.dayCountColdPeriod.length; ++i) {
                int n = i;
                total.dayCountColdPeriod[n] = total.dayCountColdPeriod[n] + r.dayCountColdPeriod[i];
            }
            for (i = 0; i < r.dayCountEmpty.length; ++i) {
                int n = i;
                total.dayCountEmpty[n] = total.dayCountEmpty[n] + r.dayCountEmpty[i];
            }
            for (i = 0; i < r.exceedingPeriods.size(); ++i) {
                total.exceedingPeriods.add(r.exceedingPeriods.get(i));
            }
            for (i = 0; i < r.exceedingEmpties.size(); ++i) {
                total.exceedingEmpties.add(r.exceedingEmpties.get(i));
            }
            int days = (int)(r.totalPeriodDuration / 120.0);
            if (days < total.totalDaysPeriod.length) {
                int n = days;
                total.totalDaysPeriod[n] = total.totalDaysPeriod[n] + 1;
                continue;
            }
            DebugLog.log("Total days Period is longer than allowed array, days = " + days * 5);
        }
        if (!this.runs.isEmpty()) {
            int div = this.runs.size();
            total.totalPeriodDuration /= (double)div;
            total.averagePeriod /= (double)div;
            total.averageEmpty /= (double)div;
            total.averageStrength /= (float)div;
            total.averageWarmStrength /= (float)div;
            total.averageColdStrength /= (float)div;
        }
        return total;
    }

    private void saveData() {
        block31: {
            if (this.runs.size() <= 0) {
                return;
            }
            try {
                FileWriter w;
                for (RunInfo r : this.runs) {
                    r.calculate();
                }
                RunInfo total = this.calculateTotal();
                String saveFile = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                ZomboidFileSystem.instance.getFileInCurrentSave("climate").mkdirs();
                File path = ZomboidFileSystem.instance.getFileInCurrentSave("climate");
                if (!path.exists() || !path.isDirectory()) break block31;
                String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("climate", saveFile + ".txt");
                DebugLog.log("Attempting to save test data to: " + fileName);
                File f = new File(fileName);
                DebugLog.log("Saving climate test data: " + fileName);
                try {
                    w = new FileWriter(f, false);
                    try {
                        this.writer = w;
                        int div = this.runs.size();
                        this.write("Simulation results." + System.lineSeparator());
                        this.write("Runs: " + this.runs.size() + ", days per cycle: " + this.durDays);
                        if (this.doOverrideSandboxRainMod) {
                            this.write("RainModifier used: " + this.sandboxRainModOverride);
                        } else {
                            this.write("RainModifier used: " + SandboxOptions.instance.getRainModifier());
                        }
                        this.write("");
                        this.write("-------------------------------------------------------------------");
                        this.write(" TOTALS OVERVIEW");
                        this.write("-------------------------------------------------------------------");
                        this.write("");
                        this.write("Total weather periods: " + total.totalPeriods + ", average per cycle: " + total.totalPeriods / div);
                        this.write("Longest weather: " + this.formatDuration(total.longestPeriod));
                        this.write("Shortest weather: " + this.formatDuration(total.shortestPeriod));
                        this.write("Average weather: " + this.formatDuration(total.averagePeriod));
                        this.write("");
                        this.write("Average total weather days per cycle: " + this.formatDuration(total.totalPeriodDuration));
                        this.write("");
                        this.write("Driest cycle total weather days: " + this.formatDuration(total.mostDryPeriod));
                        this.write("Wettest cycle total weather days: " + this.formatDuration(total.mostWetPeriod));
                        this.write("");
                        this.write("Total clear periods: " + total.totalEmpty + ", average per cycle: " + total.totalEmpty / div);
                        this.write("Longest clear: " + this.formatDuration(total.longestEmpty));
                        this.write("Shortest clear: " + this.formatDuration(total.shortestEmpty));
                        this.write("Average clear: " + this.formatDuration(total.averageEmpty));
                        this.write("");
                        this.write("Highest Front strength: " + total.highestStrength);
                        this.write("Lowest Front strength: " + total.lowestStrength);
                        this.write("Average Front strength: " + total.averageStrength);
                        this.write("");
                        this.write("Highest WarmFront strength: " + total.highestWarmStrength);
                        this.write("Lowest WarmFront strength: " + total.lowestWarmStrength);
                        this.write("Average WarmFront strength: " + total.averageWarmStrength);
                        this.write("");
                        this.write("Highest ColdFront strength: " + total.highestColdStrength);
                        this.write("Lowest ColdFront strength: " + total.lowestColdStrength);
                        this.write("Average ColdFront strength: " + total.averageColdStrength);
                        this.write("");
                        this.write("Weather period types:");
                        this.write("Normal warm: " + total.countNormalWarm + ", average: " + this.round((double)total.countNormalWarm / (double)div));
                        this.write("Normal cold: " + total.countNormalCold + ", average: " + this.round((double)total.countNormalCold / (double)div));
                        this.write("Normal storm: " + total.countStorm + ", average: " + this.round((double)total.countStorm / (double)div));
                        this.write("Normal tropical: " + total.countTropical + ", average: " + this.round((double)total.countTropical / (double)div));
                        this.write("Normal blizzard: " + total.countBlizzard + ", average: " + this.round((double)total.countBlizzard / (double)div));
                        this.write("");
                        this.write("Distribution duration in days (total periods)");
                        this.printCountTable(w, total.dayCountPeriod);
                        this.write("");
                        this.write("Distribution duration in days (WARM periods)");
                        this.printCountTable(w, total.dayCountWarmPeriod);
                        this.write("");
                        this.write("Distribution duration in days (COLD periods)");
                        this.printCountTable(w, total.dayCountColdPeriod);
                        this.write("");
                        this.write("Distribution duration in days (clear periods)");
                        this.printCountTable(w, total.dayCountEmpty);
                        this.write("");
                        this.write("Amount of weather periods exceeding threshold: " + total.exceedingPeriods.size());
                        if (!total.exceedingPeriods.isEmpty()) {
                            for (Integer integer : total.exceedingPeriods) {
                                this.writer.write(integer + " days, ");
                            }
                        }
                        this.write("");
                        this.write("");
                        this.write("Amount of clear periods exceeding threshold: " + total.exceedingEmpties.size());
                        if (!total.exceedingEmpties.isEmpty()) {
                            for (Integer integer : total.exceedingEmpties) {
                                this.writer.write(integer + " days, ");
                            }
                        }
                        this.write("");
                        this.write("");
                        this.write("Distribution duration total weather days:");
                        this.printCountTable(this.writer, total.totalDaysPeriod, 5);
                        this.writeDataExtremes();
                        this.writer = null;
                    }
                    finally {
                        w.close();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                f = ZomboidFileSystem.instance.getFileInCurrentSave("climate", saveFile + "_DATA.txt");
                try {
                    w = new FileWriter(f, false);
                    try {
                        this.writer = w;
                        this.writeData();
                        this.writer = null;
                    }
                    finally {
                        w.close();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                f = ZomboidFileSystem.instance.getFileInCurrentSave("climate", saveFile + "_PATTERNS.txt");
                try {
                    w = new FileWriter(f, false);
                    try {
                        this.writer = w;
                        this.writePatterns();
                        this.writer = null;
                    }
                    finally {
                        w.close();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private double round(double d) {
        return (double)Math.round(d * 100.0) / 100.0;
    }

    private void writeRunInfo(RunInfo r, int id) throws Exception {
        this.write("-------------------------------------------------------------------");
        this.write(" RUN NR: " + id);
        this.write("-------------------------------------------------------------------");
        this.write("");
        this.write("Total weather periods: " + r.totalPeriods);
        this.write("Longest weather: " + this.formatDuration(r.longestPeriod));
        this.write("Shortest weather: " + this.formatDuration(r.shortestPeriod));
        this.write("Average weather: " + this.formatDuration(r.averagePeriod));
        this.write("");
        this.write("Total weather days for cycle: " + this.formatDuration(r.totalPeriodDuration));
        this.write("");
        this.write("Total clear periods: " + r.totalEmpty);
        this.write("Longest clear: " + this.formatDuration(r.longestEmpty));
        this.write("Shortest clear: " + this.formatDuration(r.shortestEmpty));
        this.write("Average clear: " + this.formatDuration(r.averageEmpty));
        this.write("");
        this.write("Highest Front strength: " + r.highestStrength);
        this.write("Lowest Front strength: " + r.lowestStrength);
        this.write("Average Front strength: " + r.averageStrength);
        this.write("");
        this.write("Highest WarmFront strength: " + r.highestWarmStrength);
        this.write("Lowest WarmFront strength: " + r.lowestWarmStrength);
        this.write("Average WarmFront strength: " + r.averageWarmStrength);
        this.write("");
        this.write("Highest ColdFront strength: " + r.highestColdStrength);
        this.write("Lowest ColdFront strength: " + r.lowestColdStrength);
        this.write("Average ColdFront strength: " + r.averageColdStrength);
        this.write("");
        this.write("Weather period types:");
        this.write("Normal warm: " + r.countNormalWarm);
        this.write("Normal cold: " + r.countNormalCold);
        this.write("Normal storm: " + r.countStorm);
        this.write("Normal tropical: " + r.countTropical);
        this.write("Normal blizzard: " + r.countBlizzard);
        this.write("");
        this.write("Distribution duration in days (total periods)");
        this.printCountTable(this.writer, r.dayCountPeriod);
        this.write("");
        this.write("Distribution duration in days (WARM periods)");
        this.printCountTable(this.writer, r.dayCountWarmPeriod);
        this.write("");
        this.write("Distribution duration in days (COLD periods)");
        this.printCountTable(this.writer, r.dayCountColdPeriod);
        this.write("");
        this.write("Distribution duration in days (clear periods)");
        this.printCountTable(this.writer, r.dayCountEmpty);
        this.write("");
        this.write("Amount of weather periods exceeding threshold: " + r.exceedingPeriods.size());
        if (!r.exceedingPeriods.isEmpty()) {
            for (Integer integer : r.exceedingPeriods) {
                this.write(integer + " days.");
            }
        }
        this.write("");
        this.write("Amount of clear periods exceeding threshold: " + r.exceedingEmpties.size());
        if (!r.exceedingEmpties.isEmpty()) {
            for (Integer integer : r.exceedingEmpties) {
                this.write(integer + " days.");
            }
        }
    }

    private void write(String str) throws Exception {
        this.writer.write(str + System.lineSeparator());
    }

    private void writeDataExtremes() throws Exception {
        int id = 0;
        int dryId = -1;
        int wetId = -1;
        RunInfo mostDry = null;
        RunInfo mostWet = null;
        for (RunInfo r : this.runs) {
            ++id;
            if (mostDry == null || r.totalPeriodDuration < mostDry.totalPeriodDuration) {
                mostDry = r;
                dryId = id;
            }
            if (mostWet != null && !(r.totalPeriodDuration > mostWet.totalPeriodDuration)) continue;
            mostWet = r;
            wetId = id;
        }
        this.write("");
        this.write("MOST DRY RUN:");
        if (mostDry != null) {
            this.writeRunInfo(mostDry, dryId);
        }
        this.write("");
        this.write("MOST WET RUN:");
        if (mostWet != null) {
            this.writeRunInfo(mostWet, wetId);
        }
    }

    private void writeData() throws Exception {
        int id = 0;
        for (RunInfo r : this.runs) {
            this.writeRunInfo(r, ++id);
        }
    }

    private void writePatterns() throws Exception {
        String clear = "-";
        String weather = "#";
        String storm = "S";
        String tropical = "T";
        String blizzard = "B";
        for (RunInfo r : this.runs) {
            int id = 0;
            for (RecordInfo ri : r.records) {
                int c = (int)Math.ceil(ri.durationHours / 24.0);
                String s = ri.isWeather && ri.weatherType == 1 ? new String(new char[c]).replace("\u0000", "S") : (ri.isWeather && ri.weatherType == 2 ? new String(new char[c]).replace("\u0000", "T") : (ri.isWeather && ri.weatherType == 3 ? new String(new char[c]).replace("\u0000", "B") : (id == 0 && !ri.isWeather && c >= 2 ? new String(new char[c - 1]).replace("\u0000", "-") : new String(new char[c]).replace("\u0000", ri.isWeather ? "#" : "-"))));
                this.writer.write(s);
                ++id;
            }
            this.writer.write(System.lineSeparator());
        }
    }

    private void printCountTable(FileWriter w, int[] table) throws Exception {
        this.printCountTable(w, table, 1);
    }

    private void printCountTable(FileWriter w, int[] table, int indexMod) throws Exception {
        if (table == null || table.length <= 0) {
            return;
        }
        int t = 0;
        for (int i = 0; i < table.length; ++i) {
            if (table[i] <= t) continue;
            t = table[i];
        }
        this.write("    DAYS   COUNT GRAPH");
        float mod = 50.0f / (float)t;
        if (t > 0) {
            for (int i = 0; i < table.length; ++i) {
                Object s = "";
                s = (String)s + String.format("%1$8s", i * indexMod + "-" + (i * indexMod + indexMod));
                int v = table[i];
                s = (String)s + String.format("%1$8s", v);
                s = (String)s + " ";
                int cnt = (int)((float)v * mod);
                if (cnt > 0) {
                    s = (String)s + new String(new char[cnt]).replace("\u0000", "#");
                } else if (v > 0) {
                    s = (String)s + "*";
                }
                this.write((String)s);
            }
        }
    }

    private String formatDuration(double duration) {
        int days = (int)(duration / 24.0);
        int hours = (int)(duration - (double)(days * 24));
        return days + " days, " + hours + " hours.";
    }

    private class RunInfo {
        public double seedA;
        public int durationDays;
        public double durationHours;
        public ArrayList<RecordInfo> records;
        public double totalPeriodDuration;
        public double longestPeriod;
        public double shortestPeriod;
        public int totalPeriods;
        public double averagePeriod;
        public double longestEmpty;
        public double shortestEmpty;
        public int totalEmpty;
        public double averageEmpty;
        public float highestStrength;
        public float lowestStrength;
        public float averageStrength;
        public float highestWarmStrength;
        public float lowestWarmStrength;
        public float averageWarmStrength;
        public float highestColdStrength;
        public float lowestColdStrength;
        public float averageColdStrength;
        public int countNormalWarm;
        public int countNormalCold;
        public int countStorm;
        public int countTropical;
        public int countBlizzard;
        public int[] dayCountPeriod;
        public int[] dayCountWarmPeriod;
        public int[] dayCountColdPeriod;
        public int[] dayCountEmpty;
        public ArrayList<Integer> exceedingPeriods;
        public ArrayList<Integer> exceedingEmpties;
        public double mostWetPeriod;
        public double mostDryPeriod;
        public int[] totalDaysPeriod;
        final /* synthetic */ ClimMngrDebug this$0;

        private RunInfo(ClimMngrDebug climMngrDebug) {
            ClimMngrDebug climMngrDebug2 = climMngrDebug;
            Objects.requireNonNull(climMngrDebug2);
            this.this$0 = climMngrDebug2;
            this.records = new ArrayList();
            this.shortestPeriod = 9.99999999E8;
            this.shortestEmpty = 9.99999999E8;
            this.lowestStrength = 1.0f;
            this.lowestWarmStrength = 1.0f;
            this.lowestColdStrength = 1.0f;
            this.dayCountPeriod = new int[16];
            this.dayCountWarmPeriod = new int[16];
            this.dayCountColdPeriod = new int[16];
            this.dayCountEmpty = new int[75];
            this.exceedingPeriods = new ArrayList();
            this.exceedingEmpties = new ArrayList();
            this.mostDryPeriod = 9.99999999E8;
        }

        public RecordInfo addRecord(double duration) {
            RecordInfo record = new RecordInfo(this.this$0);
            record.durationHours = duration;
            record.isWeather = false;
            this.records.add(record);
            return record;
        }

        public RecordInfo addRecord(int airType, double duration, float strength, boolean storm, boolean tropical, boolean blizzard) {
            RecordInfo record = new RecordInfo(this.this$0);
            record.durationHours = duration;
            record.isWeather = true;
            record.airType = airType;
            record.strength = strength;
            record.weatherType = 0;
            if (storm) {
                record.weatherType = 1;
            } else if (tropical) {
                record.weatherType = 2;
            } else if (blizzard) {
                record.weatherType = 3;
            }
            this.records.add(record);
            return record;
        }

        public void calculate() {
            double totalDurPeriod = 0.0;
            double totalDurEmpty = 0.0;
            float totalStr = 0.0f;
            float totalWarmStr = 0.0f;
            float totalColdStr = 0.0f;
            int warmPeriods = 0;
            int coldPeriods = 0;
            for (RecordInfo n : this.records) {
                int days = (int)(n.durationHours / 24.0);
                if (n.isWeather) {
                    this.totalPeriodDuration += n.durationHours;
                    if (n.durationHours > this.longestPeriod) {
                        this.longestPeriod = n.durationHours;
                    }
                    if (n.durationHours < this.shortestPeriod) {
                        this.shortestPeriod = n.durationHours;
                    }
                    ++this.totalPeriods;
                    totalDurPeriod += n.durationHours;
                    if (n.strength > this.highestStrength) {
                        this.highestStrength = n.strength;
                    }
                    if (n.strength < this.lowestStrength) {
                        this.lowestStrength = n.strength;
                    }
                    totalStr += n.strength;
                    if (n.airType == 1) {
                        ++warmPeriods;
                        if (n.strength > this.highestWarmStrength) {
                            this.highestWarmStrength = n.strength;
                        }
                        if (n.strength < this.lowestWarmStrength) {
                            this.lowestWarmStrength = n.strength;
                        }
                        totalWarmStr += n.strength;
                        if (n.weatherType == 1) {
                            ++this.countStorm;
                        } else if (n.weatherType == 2) {
                            ++this.countTropical;
                        } else if (n.weatherType == 3) {
                            ++this.countBlizzard;
                        } else {
                            ++this.countNormalWarm;
                        }
                        if (days < this.dayCountWarmPeriod.length) {
                            int n2 = days;
                            this.dayCountWarmPeriod[n2] = this.dayCountWarmPeriod[n2] + 1;
                        }
                    } else {
                        ++coldPeriods;
                        if (n.strength > this.highestColdStrength) {
                            this.highestColdStrength = n.strength;
                        }
                        if (n.strength < this.lowestColdStrength) {
                            this.lowestColdStrength = n.strength;
                        }
                        totalColdStr += n.strength;
                        ++this.countNormalCold;
                        if (days < this.dayCountColdPeriod.length) {
                            int n3 = days;
                            this.dayCountColdPeriod[n3] = this.dayCountColdPeriod[n3] + 1;
                        }
                    }
                    if (days < this.dayCountPeriod.length) {
                        int n4 = days;
                        this.dayCountPeriod[n4] = this.dayCountPeriod[n4] + 1;
                        continue;
                    }
                    DebugLog.log("Period is longer than allowed array, days = " + days);
                    this.exceedingPeriods.add(days);
                    continue;
                }
                if (n.durationHours > this.longestEmpty) {
                    this.longestEmpty = n.durationHours;
                }
                if (n.durationHours < this.shortestEmpty) {
                    this.shortestEmpty = n.durationHours;
                }
                ++this.totalEmpty;
                totalDurEmpty += n.durationHours;
                if (days < this.dayCountEmpty.length) {
                    int n5 = days;
                    this.dayCountEmpty[n5] = this.dayCountEmpty[n5] + 1;
                    continue;
                }
                DebugLog.log("No-Weather period is longer than allowed array, days = " + days);
                this.exceedingEmpties.add(days);
            }
            if (this.totalPeriods > 0) {
                this.averagePeriod = totalDurPeriod / (double)this.totalPeriods;
                this.averageStrength = totalStr / (float)this.totalPeriods;
                if (warmPeriods > 0) {
                    this.averageWarmStrength = totalWarmStr / (float)warmPeriods;
                }
                if (coldPeriods > 0) {
                    this.averageColdStrength = totalColdStr / (float)coldPeriods;
                }
            }
            if (this.totalEmpty > 0) {
                this.averageEmpty = totalDurEmpty / (double)this.totalEmpty;
            }
        }
    }

    private class RecordInfo {
        public boolean isWeather;
        public float strength;
        public int airType;
        public double durationHours;
        public int weatherType;

        private RecordInfo(ClimMngrDebug climMngrDebug) {
            Objects.requireNonNull(climMngrDebug);
        }
    }
}


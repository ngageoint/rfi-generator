package controllers;

import play.*;
import play.db.jpa.*;
import play.modules.pdf.PDF.Options;
import play.mvc.*;
import play.libs.*;
import play.data.*;
import play.data.binding.*;
import play.data.validation.*;
import play.server.hybi10.Base64;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.*;

import helpers.*;
import notifiers.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.util.concurrent.atomic.*;

import javax.persistence.*;
import javax.persistence.criteria.*;

import controllers.deadbolt.*;

import models.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.joda.time.*;
import org.joda.time.format.*;

import org.yaml.snakeyaml.Yaml;

import flexjson.JSONDeserializer;
import flexjson.transformer.*;

import org.apache.commons.io.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import static play.modules.pdf.PDF.*;

/*
    Handles the routes associated w/ reporting:
        BI activities, reports page in the admin, the PDF export and dashboard
            overview graph.
*/
@With({Master.class, Deadbolt.class})
public class ReportingController extends BaseController {
    public static void chart() {
        render();
    }
    
    // Business Intelligence graph on the map experience (available via Business Intelligence dialog)
    @Restrict("!Field")
    public static void biActivities() {
        Query q = JPA.em().createQuery(
            "select new map(sum( case when a.type = 'CREATED' then 1 else 0 end) as CREATED," +
            "sum( case when a.type = 'STARTED' then 1 else 0 end) as STARTED," +
            "sum( case when a.type = 'COMPLETED' then 1 else 0 end) as COMPLETED, " + 
            "SUBSTRING(a.createdAt, 1, 10) as Date)" + 
            "from Activity a where a.type in ('CREATED', 'STARTED', 'COMPLETED') group by SUBSTRING(a.createdAt, 1, 10)");
        List<Map> movement = q.getResultList();
        
        Query q2 = JPA.em().createQuery(
            "select new map(sum(case when r.status = :p then 1 else 0 end) as NOT_STARTED," + 
            "sum( case when r.status = :iw then 1 else 0 end) as IN_PROGRESS," +
            "sum( case when r.status = :c then 1 else 0 end) as COMPLETED) from RFI r");
        q2.setParameter("p", ApplicationHelpers.PENDING);
        q2.setParameter("iw", ApplicationHelpers.IN_WORK);
        q2.setParameter("c", ApplicationHelpers.COMPLETED);
        List<Map> counts = q2.getResultList();
        
        List<Map> ret = new ArrayList<Map>();
        Map reports = new HashMap();
        reports.put("movement", movement);
        reports.put("counts", counts);

        rawSerialize(reports, true);
    }
    
    
    // Business Intelligence dialog on the map experience
    @Restrict("!Field")
    public static void bi(Long id) {
        List<Activity> activities = null;
        if ( id != null && id != 0) {
            activities = Activity.find("rfi.id = ? order by createdAt desc", id).fetch();
        }
        else {
            activities = Activity.find("order by createdAt desc").fetch();
        }
        render(activities, id);
    }

    public static void byEvent(Long event, Boolean eventArchived, Long group) {
        List<Map> byEvent = ReportingHelpers.getRFIStatusesByEvent(event, eventArchived, group);
        rawSerialize(byEvent);
    }
    
    public static void dashboardOverview(Long event, @As("yyyy-MM-dd") Date startDate, @As("yyyy-MM-dd") Date endDate, Long group) {
        rawSerialize(ReportingHelpers.getTotalsOverTime(event, startDate, endDate, group, false));
    }
    
    public static void byCreated(@As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, Long event, Long group) {
        List<Map> byCreated = ReportingHelpers.getCreatedCount(start, end, event, group);
        rawSerialize(byCreated);
    }
    
    public static void rfiTotals(Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, Long group) {
        rawSerialize(ReportingHelpers.getTotalsOverTime(event, start, end, group, true));
    }
    
    // /admin/generate - For creating a PDF report
    public static void generate(String format, Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, 
            Boolean productArchived, Boolean assigneeArchived, Boolean eventArchived, Boolean avgArchived, Long group) {
        Map map = _reports(event, start, end, productArchived, assigneeArchived, eventArchived, avgArchived, group);
        //TODO: I hate how I'm passing back a map of return values, this seems ghetto...
        Map<String, List<RFI>> assignees = (Map<String, List<RFI>>)map.get("assignees");
        String cv = (String)map.get("cv");
        String va = (String)map.get("va");
        String ac = (String)map.get("ac");
        String cc = (String)map.get("cc");
        List<Map> productFormats = (List<Map>)map.get("productFormats");
        start = (Date)map.get("start");
        end = (Date)map.get("end");

        Options options = new Options();
        options.FOOTER = "<span style='font-size: 8pt;color: #666;'>RFI Generator Report</span>" + 
                "<span style=\" color: #666;float: right;font-size: 8pt;\">Page <pagenumber>/<pagecount></span>";
        options.filename = "report.pdf";
        
        response.setHeader("Content-disposition", "attachment; filename=report.pdf");
        renderPDF(options, assignees, cv, va, ac, cc, productFormats, start, end);
    }
    
    // /admin/reports - Reports page available via "Reports" in the admin experience's main nav
    public static void reports(Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end,
            Boolean productArchived, Boolean assigneeArchived, Boolean eventArchived, Boolean avgArchived, Long group) {
        Map map = _reports(event, start, end, productArchived, assigneeArchived, eventArchived, avgArchived, group);
        Map<String, List<RFI>> assignees = (Map<String, List<RFI>>)map.get("assignees");
        String cv = (String)map.get("cv");
        String va = (String)map.get("va");
        String ac = (String)map.get("ac");
        String cc = (String)map.get("cc");
        List<Map> productFormats = (List<Map>)map.get("productFormats");
        start = (Date)map.get("start");
        end = (Date)map.get("end");
        render(assignees, cv, va, ac, cc, productFormats, start, end);
    }
    
    // Shared stuff used by both the reporting page AND the PDF report
    static Map _reports(Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, Boolean productArchived, Boolean assigneeArchived, Boolean eventArchived, Boolean avgArchived, Long group) {
        List<RFI> all = ReportingHelpers.reportRFIs(event, start, end, assigneeArchived, group);
        Map<String, List<RFI>> assignees = new HashMap<String,List<RFI>>();
        
        for ( RFI r: all) {
            if ( r.assignedTo != null && !r.assignedTo.trim().equals("")) {
                String [] users = r.assignedTo.split(ApplicationHelpers.mailSplit);
                for ( String s: users) {
                    if ( !assignees.containsKey(s)) {
                        List<RFI> tmp = new ArrayList<RFI>();
                        tmp.add(r);
                        assignees.put(s, tmp);
                    }
                    else {
                        List<RFI> tmp = assignees.get(s);
                        tmp.add(r);
                    }
                }
            }
        }
        
        List<Map> productFormats = ReportingHelpers.getProductFormatCounts(start, end, event, productArchived, group);
        
        // Figure out average length of activites
        
        // Between created and verified
        Double d_cv = ReportingHelpers.averageCreatedToValidationTime(start, end, event, avgArchived, group, true);
        Logger.info("AVG: %f seconds between created and verified", d_cv);
        
        // Between approval and assignment
        Double d_va = ReportingHelpers.averageApprovalToAssignmentTime(start, end, event, avgArchived, group, true);
        Logger.info("AVG: %f seconds between verification and assignment", d_va);
        
        // Between assigned and completed
        Double d_ac = ReportingHelpers.averageAssignmentToCompletionTime(start, end, event, avgArchived, group, true);
        Logger.info("AVG: %f seconds between assignment and completion", d_ac);
        
        // Between created and completed
        Double d_cc = ReportingHelpers.averageCreationToCompletionTime(start, end, event, avgArchived, group, true);
        Logger.info("AVG: %f seconds between creation and completion", d_ac);
        
        String cv = "N/A";
        String va = "N/A";
        String ac = "N/A";
        String cc = "N/A";
        
        PeriodFormatter b = ApplicationHelpers.getPeriodFormatter();
        
        // Mysql returns average in seconds, Duration expects milliseconds
        if ( d_cv != null) {
            Duration dur = new Duration(new Double(d_cv*1000).longValue());
            cv = b.print(dur.toPeriod());
        }
        if ( d_va != null) {
            Duration dur = new Duration(new Double(d_va*1000).longValue());
            va = b.print(dur.toPeriod());
        }
        if ( d_ac != null) {
            Duration dur = new Duration(new Double(d_ac*1000).longValue());
            ac = b.print(dur.toPeriod());
        }
        if ( d_cc != null) {
            Duration dur = new Duration(new Double(d_cc*1000).longValue());
            cc = b.print(dur.toPeriod());
        }
        
        Map ret = new HashMap();
        ret.put("assignees", assignees);
        ret.put("cv", cv);
        ret.put("va", va);
        ret.put("ac", ac);
        ret.put("cc", cc);
        ret.put("productFormats", productFormats);
        ret.put("start", start);
        ret.put("end", end);
        return ret;
    }
    
    /*
        These jFree* functions are used to replicate what is done in Javascript/jQplot
        on the front-end on the reporting page and are ONLY used in generating a PDF.
        
        See www.jfree.org/jfreechart for doc
    */
    public static String jfreeTotals(Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, Long group) throws IOException {
        List<Object[]> snapshots = ReportingHelpers.getTotalsOverTime(event, start, end, group, true);
        List<List> totals = new ArrayList<List>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        TimeSeries seriesTotal = new TimeSeries("Total");
        TimeSeries seriesOpen = new TimeSeries("Open");
        TimeSeries seriesClosed = new TimeSeries("Closed");
        TimeSeriesCollection seriesCollection = new TimeSeriesCollection();
        
        // format data
        for (Object[] item : snapshots) {
            List tempList = new ArrayList();
            tempList.add((Double)item[0]);
            tempList.add((Double)item[1]);
            tempList.add((Double)item[2]);
            try {
                tempList.add((Date)dateFormat.parse((String) item[3]));
            } catch (ParseException e) {

            }
            tempList.add((Long)item[4]);
            totals.add(tempList);
        }

        // set initial values of first item to 0.0 if null
        if ( totals != null && totals.size() > 0) {
            if (totals.get(0).get(0) == null)
            {
                totals.get(0).set(0, 0.0);
            }
            if (totals.get(0).get(1) == null)
            {
                totals.get(0).set(1, 0.0);
            }
            if (totals.get(0).get(2) == null)
            {
                totals.get(0).set(2, 0.0);
            }
        }

        // total RFI information by date
        for (int i = 0; i < totals.size() - 1; i++) {
            int j = i + 1;
            if (totals.get(i) != null) {
                while (j < totals.size() &&  totals.get(j) != null && totals.get(i).get(3) != null && totals.get(j).get(3) != null 
                        && ((Date)totals.get(i).get(3)).equals((Date)totals.get(j).get(3))) {
                    if (totals.get(j).get(0) == null)
                    {
                        totals.get(j).set(0, 0.0);
                    }
                    if (totals.get(j).get(1) == null)
                    {
                        totals.get(j).set(1, 0.0);
                    }
                    if (totals.get(j).get(2) == null)
                    {
                        totals.get(j).set(2, 0.0);
                    }
                    totals.get(i).set(0, (Double)totals.get(i).get(0) + (Double)totals.get(j).get(0));
                    totals.get(i).set(1, (Double)totals.get(i).get(1) + (Double)totals.get(j).get(1));
                    totals.get(i).set(2, (Double)totals.get(i).get(2) + (Double)totals.get(j).get(2));
                    totals.set(j, null);
                    j++;
                }
            }
        }
        totals.removeAll(Collections.singleton(null));

        // add data to appropriate series
        for (int i = 0; i < totals.size(); i++) {
            if (totals.get(i) != null) {
                seriesTotal.add(new Day((Date)totals.get(i).get(3)), (Double)totals.get(i).get(0));
                seriesOpen.add(new Day((Date)totals.get(i).get(3)), (Double)totals.get(i).get(1));
                seriesClosed.add(new Day((Date)totals.get(i).get(3)), (Double)totals.get(i).get(2));
            }
        }
        seriesCollection.addSeries(seriesTotal);
        seriesCollection.addSeries(seriesOpen);
        seriesCollection.addSeries(seriesClosed);

        JFreeChart chart = ChartFactory.createTimeSeriesChart("", "", "Count", seriesCollection, true, false, false);
         
        // chart formatting
        Color[] seriesColors = {Color.decode("#A4AAFC"), Color.decode("#373E9E"), Color.decode("#379E69")}; 
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES); 

        chart.getXYPlot().setBackgroundPaint(Color.decode("#fffdf6"));
        chart.getXYPlot().setRangeGridlineStroke(new BasicStroke());
        chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setDomainGridlineStroke(new BasicStroke());
        chart.getXYPlot().setDomainGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setDomainGridlinesVisible(true);
        DateAxis xaxis = (DateAxis) chart.getXYPlot().getDomainAxis();
        xaxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        NumberAxis yaxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        yaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        for(int i = 0; i < 3; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(3));
            renderer.setSeriesPaint(i, seriesColors[i]);
            renderer.setSeriesShape(i, new Ellipse2D.Double(-4.5, -4.5, 9.0, 9.0));
        }

        chart.getXYPlot().setRenderer(renderer);

        // get chart image data as base64
        BufferedImage img = draw(chart, 690, 410);
        byte[] imgData = ChartUtilities.encodeAsPNG(img);
        Base64 base64 = new Base64();
        return "data:image/png;base64," + base64.encode(imgData);
    }
    
    public static String jfreeByCreated(Long event, @As("yyyy-MM-dd") Date start, @As("yyyy-MM-dd") Date end, Long group) throws IOException {
        List<Map> byCreated = ReportingHelpers.getCreatedCount(start, end, event, group);
        List<TimeSeries> seriesList = new ArrayList<TimeSeries>();
        List<String> seriesLookup = new ArrayList<String>();
        TimeSeriesCollection seriesCollection = new TimeSeriesCollection();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < byCreated.size(); i++) {
        	if (byCreated.get(i) != null) {
                if (!seriesLookup.contains(byCreated.get(i).get("event").toString())) {
                    seriesList.add(new TimeSeries(byCreated.get(i).get("event").toString()));
                    seriesLookup.add(byCreated.get(i).get("event").toString());
                } 

                int index = seriesLookup.indexOf(byCreated.get(i).get("event").toString());
                try {
                    seriesList.get(index).add(new Day(dateFormat.parse(byCreated.get(i).get("date").toString())), 
                            Long.parseLong(byCreated.get(i).get("count").toString()));
                }
                catch (NumberFormatException e) { }
                catch (ParseException e) { }
            }
        }

        for (int i = 0; i < seriesList.size(); i++)
        {
            seriesCollection.addSeries(seriesList.get(i));
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart("", "", "Number created", seriesCollection, true, false, false);
        
        // chart formatting
        Color[] seriesColors = {Color.decode("#A4AAFC"), Color.decode("#373E9E"), Color.decode("#379E69"), Color.decode("#ADE394"), 
                Color.decode("#cccccc"), Color.decode("#333333"), Color.decode("#E37D76")}; 
        StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES_AND_LINES); 
        chart.getXYPlot().setBackgroundPaint(Color.decode("#fffdf6"));
        chart.getXYPlot().setRangeGridlineStroke(new BasicStroke());
        chart.getXYPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setDomainGridlineStroke(new BasicStroke());
        chart.getXYPlot().setDomainGridlinePaint(Color.LIGHT_GRAY);
        chart.getXYPlot().setDomainGridlinesVisible(true);

        DateAxis xaxis = (DateAxis) chart.getXYPlot().getDomainAxis();
        xaxis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));
        NumberAxis yaxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        yaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        for(int i = 0; i < seriesColors.length; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(3));
            renderer.setSeriesPaint(i, seriesColors[i]);
            renderer.setSeriesShape(i, new Ellipse2D.Double(-4.5, -4.5, 9.0, 9.0));
        }
        chart.getXYPlot().setRenderer(renderer);

        // get chart image data as base64
        BufferedImage img = draw(chart, 690, 410);
        byte[] imgData = ChartUtilities.encodeAsPNG(img);
        Base64 base64 = new Base64();
        return "data:image/png;base64," + base64.encode(imgData);
    }
    
    public static String jfreeByEvent(Long event, Boolean archived, Long group) throws IOException {
        List<Map> byEvent = ReportingHelpers.getRFIStatusesByEvent(event, archived, group);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (int i = 0; i < byEvent.size(); i++) {
            if (byEvent.get(i) != null) {
                Object eventName = byEvent.get(i).get("event_name");
                dataset.addValue(Long.parseLong(byEvent.get(i).get("not_verified").toString()), "RFIs Pending", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("accepted").toString()), "RFIs Accepted", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("in_progress").toString()), "RFIs In Work", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("completed").toString()), "RFIs Completed", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("canceled").toString()), "RFIs Canceled", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("persistent").toString()), "RFIs Persistent", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("on_hold").toString()), "RFIs On Hold", eventName.toString());
                dataset.addValue(Long.parseLong(byEvent.get(i).get("with_customer").toString()), "RFIs With Customer", eventName.toString());
            }
        }

        JFreeChart chart = ChartFactory.createStackedBarChart("", "", "Total RFIs", dataset, PlotOrientation.VERTICAL, true, false, false);

        // chart formatting
        Color[] seriesColors = {Color.decode("#cccccc"), Color.decode("#cc3333"), Color.decode("#f89406"), Color.decode("#468847"), Color.decode("#999999"), Color.decode("#666666"), Color.decode("#333333"), Color.decode("#000000")}; 
        StackedBarRenderer renderer = new StackedBarRenderer();
        BarPainter barPainter = new StandardBarPainter();
        renderer.setBarPainter(barPainter);
        chart.getCategoryPlot().setBackgroundPaint(Color.decode("#fffdf6"));
        chart.getCategoryPlot().setRangeGridlineStroke(new BasicStroke());
        chart.getCategoryPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);

        for(int i = 0; i < seriesColors.length; i++) {
            renderer.setSeriesPaint(i, seriesColors[i]);
        }
        chart.getCategoryPlot().setRenderer(renderer);

        // get chart image data as base64
        BufferedImage img = draw(chart, 690, 410);
        byte[] imgData = ChartUtilities.encodeAsPNG(img);
        Base64 base64 = new Base64();
        return "data:image/png;base64," + base64.encode(imgData);
    }
    
    static BufferedImage draw(JFreeChart chart, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
        g2.dispose();
        return img;
    }
}

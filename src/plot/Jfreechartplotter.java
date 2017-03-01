package plot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.text.Document;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class Jfreechartplotter {

	/*private class CustomTickUnitSource implements TickUnitSource{

		@Override
		public TickUnit getCeilingTickUnit(TickUnit arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TickUnit getCeilingTickUnit(double arg0) {
			(Math.round(arg0) % 5)*5
			return null;
		}

		@Override
		public TickUnit getLargerTickUnit(TickUnit arg0) {
			// TODO Auto-generated method stub
			return null;
		}
  	  
    }*/
	
	private String destinationDirectory;
	
	public Jfreechartplotter(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}
	
	public void plotXYLine100x100(String title, String xlabel, String ylabel, float[] xvalues, float[] yvalues){
			if(xvalues.length == 0 || ( xvalues.length != yvalues.length) ) {
				System.out.println("Error, the arrays of the x and y values do not have the same values, or they are empty");
				return;
			}
	       XYSeries series = new XYSeries(title);
	       for(int i = 0; i < xvalues.length; i++){
	    	   series.add(xvalues[i], yvalues[i]);
	    	   
	       }
	      XYSeriesCollection dataset = new XYSeriesCollection();
	      dataset.addSeries(series);
	      
	      JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            xlabel,
            ylabel,
            dataset,
            PlotOrientation.VERTICAL,
            true, // display legend
            false, // tooltips
            false // urls
        );
	      chart.getXYPlot().getDomainAxis().setRange(0, 100);
	      chart.getXYPlot().getRangeAxis().setRange(0, 100);
	      chart.getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());;
	      //chart.getXYPlot().getDomainAxis().setStandardTickUnits(new );
	     try {
			saveToFile(chart,title,1300,1000,1f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void plotScatterPlot(String title, String xlabel, String ylabel, Object[] xvalues, Object[] yvalues, Object[] yvalues2, Object[] yvalues3, String[] seriesLabel){
		if(xvalues.length == 0 || ( xvalues.length != yvalues.length) ) {
			System.out.println("Error, the arrays of the x and y values do not have the same values, or they are empty");
			return;
		}
		if(xvalues.length == 0 || ( xvalues.length != yvalues2.length) ) {
			System.out.println("Error, the arrays of the x and y values do not have the same values, or they are empty");
			return;
		}
		if(xvalues.length == 0 || ( xvalues.length != yvalues3.length) ) {
			System.out.println("Error, the arrays of the x and y values do not have the same values, or they are empty");
			return;
		}
		XYSeries series1 = new XYSeries(seriesLabel[0]);
		XYSeries series2 = new XYSeries(seriesLabel[1]);
		XYSeries series3 = new XYSeries(seriesLabel[2]);
		
		 //double[][] data = new double[2][xvalues.length];
		 for(int i = 0; i < xvalues.length; i++){
			 series1.add((float)xvalues[i], (float)yvalues[i]+((double)(Math.random())));
			 //data[0][i] = (double) xvalues[i];
			 //data[1][i] = (double) yvalues[i]+((double)(Math.random()-Math.random()));
		 }
		 //double[][] data2 = new double[2][xvalues.length];
		 for(int i = 0; i < xvalues.length; i++){
			 series2.add((float)xvalues[i], (float)yvalues2[i]+((double)(-Math.random())));
			 //data[0][i] = (double) xvalues[i];
			 //data[1][i] = (double) yvalues[i]+((double)(Math.random()-Math.random()));
		 }
		 for(int i = 0; i < xvalues.length; i++){
			 series3.add((float)xvalues[i], (float)yvalues3[i]+((double)(Math.random()-Math.random())));
			 //data[0][i] = (double) xvalues[i];
			 //data[1][i] = (double) yvalues[i]+((double)(Math.random()-Math.random()));
		 }
		 XYSeriesCollection  dataset = new XYSeriesCollection();
		 dataset.addSeries(series1);
		 dataset.addSeries(series2);
		 dataset.addSeries(series3);
		 
		JFreeChart chart = ChartFactory.createXYLineChart(
				title,
				xlabel,
				ylabel,
	            dataset,
	            PlotOrientation.VERTICAL,
	            true,
	            false,
	            false
	        );
		 XYPlot plot = (XYPlot) chart.getPlot();
		 plot.setDomainAxis(new LogarithmicAxis(xlabel));
	        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	        int size = 1;
	        renderer.setSeriesLinesVisible(0, false);
	        renderer.setSeriesShapesVisible(0, true);
	        renderer.setSeriesShape(0, new Ellipse2D.Float(-size/2, -size/2, size, size));
	        renderer.setSeriesLinesVisible(1, false);
	        renderer.setSeriesShapesVisible(1, true);   
	        renderer.setSeriesShape(1, new Ellipse2D.Float(-size/2, -size/2, size, size));
	        renderer.setSeriesLinesVisible(2, false);
	        renderer.setSeriesShapesVisible(2, true);   
	        renderer.setSeriesShape(2, new Ellipse2D.Float(-size, -size, size*2, size*2));
	        plot.setRenderer(renderer);
	        plot.setBackgroundPaint(Color.WHITE);
		
		/*XYSeries series1 = new XYSeries("Planned",);
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		JFreeChart chart = ChartFactory.createXYChart(
				title,
				xlabel,
				ylabel,
				dataset,
				true);
		
		ValueAxis domainAxis = new ValueAxis(xlabel);
        ValueAxis rangeAxis = new ValueAxis(ylabel);
        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("first",data);
        XYPlot plot = new XYPlot(dataset,domainAxis,rangeAxis);
		
		JFreeChart chart = new JFreeChart(title, plot);*/
		
     try {
		saveToFile(chart,title,1500,750,1f);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
	
	public void saveToFile(JFreeChart chart,
		    String aFileName,
		    int width,
		    int height,
		    float quality)
		    throws FileNotFoundException, IOException
		    {
		        BufferedImage img = draw( chart, width, height );
		 
		        FileOutputStream fos = new FileOutputStream(destinationDirectory+aFileName+".jpg");
		        JPEGImageEncoder encoder2 =
		        JPEGCodec.createJPEGEncoder(fos);
		        JPEGEncodeParam param2 =
		        encoder2.getDefaultJPEGEncodeParam(img);
		        param2.setQuality(quality, true);
		        encoder2.encode(img,param2);
		        fos.close();
		    }
	
	protected static BufferedImage draw(JFreeChart chart, int width, int height)
    {
        BufferedImage img =
        new BufferedImage(width , height,
        BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
                       
        chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
 
        g2.dispose();
        return img;
    }
	
}

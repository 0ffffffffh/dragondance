package dragondance.eng;

import java.awt.Color;
import java.io.FileNotFoundException;

import dragondance.Globals;
import dragondance.eng.session.SessionManager;

public class Painter {
	
	private static final float BRIGHTNESS = 1.0f;
	private static final float SATURATION = 0.2f;
	
	public static final int CP_USE_MAX_DENSITY = 0;
	public static final int CP_USE_THRESHOLD_VALUE = 1;
	
	public static final int PAINT_MODE_DEFAULT=0;
	public static final int PAINT_MODE_INTERSECTION=1;
	public static final int PAINT_MODE_MAX=PAINT_MODE_INTERSECTION;
	
	
	private int colorPolicy;
	private boolean testSampleGen=false;
	private int testMaxDensity=0;
	private int mode=PAINT_MODE_DEFAULT;
	
	public Painter() {
		this(false);
	}
	
	public Painter(boolean testSampleMode) {
		this.testSampleGen=testSampleMode;
		this.colorPolicy = CP_USE_THRESHOLD_VALUE;
	}
	
	//Hsb to Rgb conversion algorithm
	//#https://en.wikipedia.org/wiki/HSL_and_HSV#From_HSV
	private Color hsbToRgb(float hue, float sat, float brig) {
		float c = sat * brig;
		float m = brig-c;
		
		float r,g,b;
		
		hue /= 60.0;
		
		int i = ((Double)Math.floor(hue)).intValue();
		
		float x = c * (1-Math.abs(hue % 2 - 1));
		
		switch (i) {
		case 0:
			r=c;
			g=x;
			b=0;
			break;
		case 1:
			r = x;
			g = c;
			b = 0;
			break;
		case 2:
			r = 0;
			g = c;
			b = x;
			break;
		case 3:
			r = 0;
			g = x;
			b = c;
			break;
		case 4:
			r = x;
			g = 0;
			b = c;
			break;
		default:
			r = c;
			g = 0;
			b = x;
			break;
		}
		
		r = (r + m) * 255.0f;
		g = (g + m) * 255.0f;
		b = (b + m) * 255.0f;
		
		return new Color((int)r,(int)g,(int)b);
	}
	
	private int getMaxDensity() {
		if (this.testSampleGen)
			return this.testMaxDensity;
		
		return SessionManager.
				getActiveSession().
				getActiveCoverage().getMaxDensity();
	}
	
	private Color getHeatColorThreshold(int density) {
		int maxDensity;
		float heatHue,factor;
		
		maxDensity = getMaxDensity();
		
		factor = (Globals.MAX_HUE / maxDensity) / 2;
		
		if (factor < 1.0f)
			factor = 1.0f;
		
		heatHue = Globals.MIN_HUE + (density * factor);
		
		if (heatHue > Globals.MAX_HUE)
			heatHue = Globals.MAX_HUE;
		
		return hsbToRgb(heatHue,SATURATION,BRIGHTNESS);
	}
	
	private Color getHeatColorMaxDensity(int density) {
		float heatHue, dp,hp,hdiff;
		
		hdiff = Globals.MAX_HUE - Globals.MIN_HUE;
		
		dp = (density / (float)getMaxDensity()) * 100.0f;
		hp = (dp * hdiff) / 100.0f;
		heatHue = Globals.MIN_HUE + hp;
		
		return hsbToRgb(heatHue,SATURATION,BRIGHTNESS);
	}
	
	private Color getHeatColor(int density) {
		
		switch (this.colorPolicy) {
		case Painter.CP_USE_MAX_DENSITY:
			return getHeatColorMaxDensity(density);
		case Painter.CP_USE_THRESHOLD_VALUE:
			return getHeatColorThreshold(density);
		}
		
		return Color.WHITE;
	}
	
	public boolean paint(InstructionInfo inst) {
		Color color;
		
		if (this.mode == PAINT_MODE_DEFAULT) {
			color = getHeatColor(inst.getDensity());
		}
		else {
			color = hsbToRgb(360.0f,0.72f,0.60f);
		}
		
		return DragonHelper.setInstructionBackgroundColor(inst.getAddr(), color);
	}
	
	public int setMode(int newMode) {
		
		int oldMode = this.mode;
		
		if (newMode < 0 || newMode > PAINT_MODE_MAX)
			return -1;
		
		this.mode = newMode;
		return oldMode;
	}
	
	//Dont use this method. 
	public void generateTestSample(int maxDensity, String file) {
		this.testMaxDensity=maxDensity;
		
		java.io.File ff = new java.io.File(file);
		java.io.FileOutputStream fos = null;
		
		try {
			fos = new java.io.FileOutputStream(ff,true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		java.io.PrintWriter pw = new java.io.PrintWriter(fos);
		
		for (int i=1;i<this.testMaxDensity+1;i++) {
			pw.write("<div style=\"background-color: ");
			Color c = getHeatColor(i);
			String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
			pw.write(hex + "; width: 50px; height: 50px;\" />");
			pw.write(String.valueOf(i));
			pw.write("<br /><br /><br />");
			
		}
		
		pw.close();
		
	}
}

package dragondance.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class TextGraphic {
	
	class RenderItem {
		public static final int RI_TEXTOUT=0;
		public static final int RI_INCRPOS=1;
		public static final int RI_PUSHPOPFONT=2;
		public static final int RI_PUSHPOPCOLOR=3;
		public static final int RI_NEWLINE=4;
		public static final int RI_FLOATTEXTBLOCK=5;
		
		private int type;
		private Object[] v;
		
		public RenderItem(int t, Object...args) {
			this.type=t;
			this.v = args;
		}
	}
	
	private int w,h;
	private BufferedImage img;
	private Graphics gph;
	
	private Point pos;
	private int lastStringHeight=0;
	private List<RenderItem> items;
	private Stack<Font> fontStack;
	private Stack<Color> colorStack;
	private int textMaxWidth=0,textMaxHeight=0;
	private Font currFont;
	private Color currColor;
	private Color backgroundColor;
	private Point virtCoord;
	
	public TextGraphic(int width, int height, Color bgColor) {
		this.w = width;
		this.h = height;
		
		this.backgroundColor = bgColor;
		
		this.img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		this.gph = this.img.getGraphics();
		
		this.items = new ArrayList<RenderItem>();
		this.fontStack = new Stack<Font>();
		this.colorStack = new Stack<Color>();
		
		this.pos = new Point(0,0);
		
		this.virtCoord = new Point(0,this.gph.getFontMetrics().getAscent());
		
		this.currColor = this.gph.getColor();
		this.currFont = this.gph.getFont();
		
		clearScene();
	}
	
	private Point logicalToPhysical(Point p) {
		Point np = new Point();
		
		np.x = this.virtCoord.x + p.x;
		np.y = this.virtCoord.y + p.y;
		
		return np;
	}
	
	private TextGraphic newRender(int type, Object...args) {
		items.add(new RenderItem(type,args));
		return this;
	}
	
	public TextGraphic textOut(String format, Color color, Object ...args) {
		return textOut(String.format(format, args),color);
	}
	
	public TextGraphic textOut(String s, Color color) {
		return newRender(RenderItem.RI_TEXTOUT,s,color);
	}
	
	public TextGraphic incrementPos(int x, int y) {
		return newRender(RenderItem.RI_INCRPOS,x,y);
	}
	
	public TextGraphic decrementPos(int x, int y) {
		return incrementPos(-x,-y);
	}
	
	public TextGraphic pushFont(String fontName, int size, int style) {
		return newRender(RenderItem.RI_PUSHPOPFONT,fontName,size,style);
	}
	
	public TextGraphic popFont() {
		return newRender(RenderItem.RI_PUSHPOPFONT);
	}
	
	public TextGraphic pushColor(Color color) {
		return newRender(RenderItem.RI_PUSHPOPCOLOR,color);
	}
	
	public TextGraphic popColor() {
		return newRender(RenderItem.RI_PUSHPOPCOLOR);
	}
	
	public TextGraphic newLine() {
		return newRender(RenderItem.RI_NEWLINE);
	}
	
	public TextGraphic floatTextBlock() {
		return newRender(RenderItem.RI_FLOATTEXTBLOCK);
	}
	
	
	private void clearScene() {
		Color prev = this.gph.getColor();
		this.gph.setColor(this.backgroundColor);
		this.gph.fillRect(0, 0, this.w,this.h);
		
		/*
		this.gph.setColor(Color.BLACK);
		
		
		this.gph.drawLine(0, 0, this.w, 0); //top line
		this.gph.drawLine(0, 0, 0, this.h); //left line
		this.gph.drawLine(0, this.h-1, this.w-1, this.h-1); //bottom line
		this.gph.drawLine(this.w-1, 0, this.w-1,this.h-1); //right line
		*/
		
		this.gph.setColor(prev);
		
		
	}
	
	private void renderText(RenderItem r) {
		String s;
		Color prev,color;
		
		s = (String)r.v[0];
		color = (Color)r.v[1];
		
		prev = this.gph.getColor();
		
		FontMetrics fm = this.gph.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(s, this.gph);
		
		this.gph.setColor(color);
		
		Point rpos = logicalToPhysical(this.pos);
		
		
		this.gph.drawString(s, rpos.x, rpos.y);
		this.gph.setColor(prev);
		
		
		this.lastStringHeight = (int)rect.getHeight();
		
		if (this.pos.x + rect.getWidth() > this.textMaxWidth)
			this.textMaxWidth += this.pos.x + (int)rect.getWidth();
		
		if (this.pos.y + rect.getHeight() > this.textMaxHeight)
			this.textMaxHeight += this.pos.y + (int)rect.getHeight();
		
		this.pos.x += (int)rect.getWidth();
	}
	
	private void incrPos(RenderItem r) {
		int x,y;
		
		x = ((Integer)r.v[0]).intValue();
		y = ((Integer)r.v[1]).intValue();
		
		this.pos.x += x;
		this.pos.y += y;
	}
	
	private void pushPopFont(RenderItem r) {
		String fontName;
		int fontSize,style;
		
		if (r.v.length > 0) {
			fontName = (String)r.v[0];
			fontSize = ((Integer)r.v[1]).intValue();
			style = ((Integer)r.v[2]).intValue();
			
			Font f = new Font(fontName,style,fontSize);
			fontStack.push(currFont);
			this.currFont = f;
		}
		else {
			if (!fontStack.empty())
				this.currFont = fontStack.pop();
		}
		
		this.gph.setFont(this.currFont);
	}
	
	private void pushPopColor(RenderItem r) {
		Color color;
		
		if (r.v.length > 0) {
			color = (Color)r.v[0];
			colorStack.push(this.currColor);
			this.currColor=color;
		}
		else {
			if (!colorStack.empty())
				this.currColor = colorStack.pop();
		}
		
		this.gph.setColor(this.currColor);
	}
	
	private void doNewLine() {
		this.pos.x=0;
		this.pos.y += this.lastStringHeight;
	}
	
	private void doFloatBlock() {
		if (this.pos.x + this.textMaxWidth > this.w - 10) {
			//float down the block
			this.virtCoord.x = 0;
			this.virtCoord.y = this.textMaxHeight;
			
		}
		else {
			this.virtCoord.x = this.textMaxWidth;
			this.virtCoord.y = this.gph.getFontMetrics().getAscent();
		}
		
		this.pos.x=0;
		this.pos.y=0;
	}
	
	public void clear() {
		fontStack.clear();
		colorStack.clear();
		items.clear();
		
		this.pos.setLocation(0, 0);
		this.virtCoord.setLocation(0, this.gph.getFontMetrics().getAscent());
		
		this.textMaxHeight=0;
		this.textMaxWidth=0;
		
	}
	
	public void render(boolean clearAfter) {
		
		clearScene();
		
		for (RenderItem r : this.items) {
			switch (r.type) {
			case RenderItem.RI_TEXTOUT:
				renderText(r);
				break;
			case RenderItem.RI_INCRPOS:
				incrPos(r);
				break;
			case RenderItem.RI_PUSHPOPFONT:
				pushPopFont(r);
				break;
			case RenderItem.RI_PUSHPOPCOLOR:
				pushPopColor(r);
				break;
			case RenderItem.RI_NEWLINE:
				doNewLine();
				break;
			case RenderItem.RI_FLOATTEXTBLOCK:
				doFloatBlock();
				break;
			default:
				break;
			}
		}
		
		if (clearAfter)
			clear();
	}
	
	public void dispatch(Graphics g) {
		g.drawImage(this.img,0,0,this.w,this.h,null);
	}
}

package trader.support.display.tag;

import java.text.MessageFormat;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

public class PercentColumnDecorator implements DisplaytagColumnDecorator {

	public Object decorate(Object object, PageContext pageContext, MediaTypeEnum mediaTypeEnum) throws DecoratorException {
		if (object != null) {
			//return NumberFormat.getPercentInstance().format((Double) object);
			return MessageFormat.format("{0,number,0.00%}", object);
		}
		return object;
	}

}

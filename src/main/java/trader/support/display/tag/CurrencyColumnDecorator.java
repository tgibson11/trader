package trader.support.display.tag;

import java.text.NumberFormat;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

public class CurrencyColumnDecorator implements DisplaytagColumnDecorator {

	public Object decorate(Object object, PageContext pageContext, MediaTypeEnum mediaTypeEnum) throws DecoratorException {
		if (object != null) {
			return NumberFormat.getCurrencyInstance().format(object);
		}
		return object;
	}

}

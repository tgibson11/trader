package trader.support.display.tag;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

public class DateColumnDecorator implements DisplaytagColumnDecorator {

	public Object decorate(Object object, PageContext pageContext, MediaTypeEnum mediaTypeEnum) throws DecoratorException {
		if (object != null && object instanceof Date) {
			return new SimpleDateFormat("MM/dd/yyyy").format((Date) object);
		}
		return object;
	}

}

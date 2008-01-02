import java.util.*;

public class PossibleLink
{
    private SQLField leftField;
    private SQLField rightField;

    public PossibleLink (SQLField aLeftField, SQLField aRightField)
    {
        leftField = aLeftField;
        rightField = aRightField;
    }

    public SQLField getOtherField(SQLField a)
    {
        SQLField retVal = null;
        if (leftField.equals(a))
        {
            retVal = rightField;
        }
        if (rightField.equals(a))
        {
            retVal = leftField;
        }
        return retVal;
    }
}

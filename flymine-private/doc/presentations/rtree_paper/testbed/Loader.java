import java.io.IOException;

public interface Loader
{
    public IndexPage load(String indexFile, PenaltyCalculator penaltyCalc, SplitCalculator splitCalc, int pageSize) throws IOException;
}

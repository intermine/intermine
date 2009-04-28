public class SplitPage
{
    private IndexPage left, right;

    public SplitPage(IndexPage left, IndexPage right) {
        this.left = left;
        this.right = right;
    }

    public IndexPage getLeft() {
        return left;
    }

    public IndexPage getRight() {
        return right;
    }
}

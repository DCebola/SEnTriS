package pt.fct.nova.id.srv.presentation.dto;


public class UploadRequestBody {

    private final byte [] contents;
    private final String syntax;

    public UploadRequestBody(byte[] contents, String syntax) {
        this.contents = contents;
        this.syntax = syntax;
    }

    public byte[] getContents() {
        return contents;
    }

    public String getSyntax() {
        return syntax;
    }
}

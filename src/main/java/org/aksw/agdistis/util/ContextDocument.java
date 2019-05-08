package org.aksw.agdistis.util;

import java.util.ArrayList;
import java.util.List;

public class ContextDocument implements Comparable<ContextDocument>{

    private List<String> context;
    private List<String> surfaceForm;
    private String uri;
    private int uriCount;
    public ContextDocument(String uri){
        this.context=new ArrayList<String>();
        this.surfaceForm=new ArrayList<String>();
        this.uri=uri;
        this.uriCount=1;
    }
    public ContextDocument(String uri, List<String> surfaceForm,List<String>context,int uriCount){
        this.context=context;
        this.surfaceForm=surfaceForm;
        this.uri=uri;
        this.uriCount=uriCount;
    }
    public void addContext(String text){
        context.add(text);
    }
    public void addSurfaceForm(String text){
        surfaceForm.add(text);
    }
    public List<String> getContext() {
        return context;
    }

    public List<String> getSurfaceForm() {
        return surfaceForm;
    }

    public String getUri() {
        return uri;
    }

    public int getUriCount() {
        return uriCount;
    }

    public void setUriCount(int uriCount) {
        this.uriCount = uriCount;
    }
    @Override
    public int compareTo(ContextDocument o) {
        return new Integer(o.getUriCount()).compareTo(uriCount);
    }
}

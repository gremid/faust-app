package de.faustedition;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import de.faustedition.facsimile.InternetImageServer;
import de.faustedition.graph.NodeWrapperCollection;
import de.faustedition.graph.NodeWrapperCollectionTemplateModel;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Templates extends freemarker.template.Configuration {

    private static final List<Variant> VARIANTS = Variant.VariantListBuilder.newInstance()
            .mediaTypes(MediaType.TEXT_HTML_TYPE)
            .languages(Locale.GERMAN, Locale.ENGLISH)
            .add()
            .build();

    @Inject
    public Templates(Configuration configuration) {
        super();
        try {
            final String templateRootPath = configuration.property("faust.template_root");
            setTemplateLoader(templateRootPath.isEmpty()
                    ? new ClassTemplateLoader(getClass(), "/template")
                    : new FileTemplateLoader(new File(templateRootPath))
            );
            setSharedVariable("cp", configuration.property("faust.context_path"));
            setSharedVariable("yp", configuration.property("faust.yui_path"));
            setSharedVariable("facsimileIIPUrl", InternetImageServer.BASE_URI.toString());
            setSharedVariable("debug", Boolean.parseBoolean(configuration.property("faust.development_mode")));

            setAutoIncludes(Collections.singletonList("/header.ftl"));
            setDefaultEncoding("UTF-8");
            setOutputEncoding("UTF-8");
            setURLEscapingCharset("UTF-8");
            setWhitespaceStripping(true);

            setObjectWrapper(new CustomObjectWrapper());
        } catch (TemplateModelException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public Response render(Request request, ViewAndModel viewAndModel) {
        try {
            final Variant variant = Objects.firstNonNull(request.selectVariant(VARIANTS), VARIANTS.get(0));

            viewAndModel.put("message", ResourceBundle.getBundle("messages", variant.getLanguage()));

            final StringWriter entity = new StringWriter();
            getTemplate(viewAndModel.getViewName() + ".ftl").process(viewAndModel, entity);

            return Response.ok().variant(variant).entity(entity.toString()).build();
        } catch (TemplateException e) {
            throw new WebApplicationException(e);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    public static class ViewAndModel extends HashMap<String, Object> {

        private final String viewName;

        public ViewAndModel(String viewName) {
            this.viewName = viewName;
        }

        public ViewAndModel add(String key, Object value) {
            put(key, value);
            return this;
        }

        public String getViewName() {
            return viewName;
        }
    }

    public static class CustomObjectWrapper extends DefaultObjectWrapper {

        @Override
        public TemplateModel wrap(Object obj) throws TemplateModelException {
            if (obj instanceof NodeWrapperCollection) {
                return new NodeWrapperCollectionTemplateModel((NodeWrapperCollection<?>) obj);
            }
            return super.wrap(obj);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object unwrap(TemplateModel model, Class hint) throws TemplateModelException {
            if (model instanceof NodeWrapperCollectionTemplateModel) {
                return ((NodeWrapperCollectionTemplateModel) model).getCollection();
            }
            return super.unwrap(model, hint);
        }
    }


}

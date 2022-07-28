package de.flozo.cvgen;

import de.flozo.common.dto.appearance.ElementStyle;
import de.flozo.common.dto.appearance.Layer;
import de.flozo.common.dto.content.Address;
import de.flozo.common.dto.content.LetterContent;
import de.flozo.common.dto.latex.DocumentClass;
import de.flozo.common.dto.latex.LatexPackage;
import de.flozo.common.dto.latex.TikzLibrary;
import de.flozo.db.*;
import de.flozo.latex.assembly.PackageList;
import de.flozo.latex.assembly.Preamble;
import de.flozo.latex.assembly.LayerList;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Main {

    // constants
    public static final String APPLICATION_NAME = "CVgen";
    public static final String VERSION_NUMBER = "0.1";
    public static final String VERSION_DATE = "2022-07-28";

    public static final String REPO_URL = String.format("https://github.com/flozo/%1$s",
            APPLICATION_NAME);
    public static final String VERSION_INFO_LATEX_HEADER = String.format("%% =====  LaTeX code generated by %1$s v%2$s (%3$s)\n%% =====  %1$s by flozo (%4$s)",
            APPLICATION_NAME, VERSION_NUMBER, VERSION_DATE, REPO_URL);
    public static final String VERSION_INFO_PDF_META_DATA = String.format("%1$s v%2$s (%3$s); visit %4$s",
            APPLICATION_NAME, VERSION_NUMBER, VERSION_DATE, REPO_URL);


    public static void main(String[] args) {


//    Datasource2.INSTANCE.getConnection();

        Datasource2 datasource2 = Datasource2.INSTANCE;
        Connection connection = datasource2.getConnection();

        try {

            LetterContentDAO letterContentDAO = new LetterContentDAOImpl(datasource2, connection);
            LetterContent letterContent = letterContentDAO.get("test");
            System.out.println(letterContent);


            Address receiver = letterContent.getReceiver();
            Address sender = letterContent.getSender();

            ContentElement receiverNameLine = new ContentElement.Builder()
                    .addComponent(receiver.getFirstName())
                    .addComponent(receiver.getLastName())
                    .inlineDelimiter(" ")
                    .build();
            ContentElement receiverStreetLine = new ContentElement.Builder()
                    .addComponent(receiver.getStreet())
                    .addComponent(receiver.getHouseNumber())
                    .inlineDelimiter(" ")
                    .build();
            ContentElement receiverCityLine = new ContentElement.Builder()
                    .addComponent(receiver.getPostalCode())
                    .addComponent(receiver.getCity())
                    .inlineDelimiter(" ")
                    .build();
            ContentElement addressFieldText = new ContentElement.Builder()
                    .addComponent(receiverNameLine.inline())
                    .addComponent(receiverStreetLine.inline())
                    .addComponent(receiverCityLine.inline())
                    .build();
            System.out.println(addressFieldText.multiline());

            ContentElement dateField = new ContentElement.Builder()
                    .addComponent(sender.getCity())
                    .addComponent(letterContent.getDate())
                    .inlineDelimiter(", ")
                    .build();
            System.out.println(dateField.inline());


            ElementStyleDAO elementStyleDAO = new ElementStyleDAOImpl(datasource2, connection);
            ElementStyle senderField = elementStyleDAO.get("sender_field");

            ContentElement senderNameLine = new ContentElement.Builder()
                    .addComponent(sender.getFirstName())
                    .addComponent(sender.getLastName())
                    .inlineDelimiter(" ")
                    .build();

//            LatexCommandDAO latexCommandDAO = new LatexCommandDAOImpl(datasource2, connection);
//            LatexCommand documentclass = latexCommandDAO.get("documentclass");
//            LatexCommand usepackage = latexCommandDAO.get("usepackage");
//            LatexCommand usetikzlibrary = latexCommandDAO.get("usetikzlibrary");
//            LatexCommand standaloneenv = latexCommandDAO.get("standaloneenv");
//            LatexCommand node = latexCommandDAO.get("node");
//
//            LatexPackage standard = latexPackageDAO.get("latex_standard");


//            System.out.println(senderField.toString());
//            System.out.println(senderField.getPosition());
//            System.out.println(senderField.getAnchor());

//            AddressDAO senderDAO = new AddressDAOImpl(datasource2, connection);
//            Address sender = senderDAO.get(2);


//            GenericCommand documentclass = Documentclass.createWithOptions(DocumentClassName.STANDALONE, "12pt", "tikz", "multi", "crop");

            DocumentClassDAO documentClassDAO = new DocumentClassDAOImpl(datasource2, connection);
            DocumentClass documentClass = documentClassDAO.getAllIncluded().get(0);

            LatexPackageDAO latexPackageDAO = new LatexPackageDAOImpl(datasource2, connection);
            List<LatexPackage> latexPackages = latexPackageDAO.getAllIncluded();
            PackageList packageList = new PackageList(latexPackages);

            TikzLibraryDAO tikzLibraryDAO = new TikzLibraryDAOImpl(datasource2, connection);
            List<String> tikzLibraries = tikzLibraryDAO.getAll().stream().map(TikzLibrary::getName).collect(Collectors.toList());

            String pdfSubject = "Application";
            String pdfTitle = "Application";
            List<String> hyperOptions = new ArrayList<>();
            hyperOptions.add("colorlinks=true");
            hyperOptions.add("urlcolor=Blues-K");
            hyperOptions.add(String.format("pdftitle={%s}", pdfTitle));
            hyperOptions.add(String.format("pdfsubject={%s}", pdfSubject));
            hyperOptions.add(String.format("pdfauthor={%s}", senderNameLine.inline()));
            hyperOptions.add(String.format("pdfdate={%s}", LocalDate.now()));
            hyperOptions.add(String.format("pdfproducer={%s}", VERSION_INFO_PDF_META_DATA));
            hyperOptions.add(String.format("pdfcontactcity={%s}", sender.getCity()));
            hyperOptions.add(String.format("pdfcontactcountry={%s}", sender.getCountry()));
            hyperOptions.add(String.format("pdfcontactemail={%s}", sender.getEMailAddress()));


            Preamble preamble = Preamble.create(documentClass, packageList, tikzLibraries, hyperOptions);
            for (String line : preamble.getPreambleCode()) {
                System.out.println(line);
            }

            LayerDAO layerDAO = new LayerDAOImpl(datasource2, connection);
            List<String> layers = layerDAO.getAll().stream().map(Layer::getName).collect(Collectors.toList());
            LayerList layerList = new LayerList.Builder(layers).build();
            List<String> layerDeclarationBlock = layerList.getLayerCode();

            for (String layer : layerDeclarationBlock) {
                System.out.println(layer);
            }


//            PackageList packageList = new PackageList(documentclass);
//            packageList.add(PackageName.INPUTENC, "utf8")
//                    .add(PackageName.FONTENC, "T1")
//                    .add(PackageName.BABEL, "german")
//                    .add(PackageName.HYPERXMP)
//                    .add(PackageName.FIRASANS, "sfdefault", "scaled=1.0098")
//                    .add(PackageName.NEWTXSF)
//                    .add(PackageName.FONTAWESOME5)
//                    .add(PackageName.CSQUOTES, "autostyle=true")
//                    .add(PackageName.ENUMITEM)
//                    .add(PackageName.MICROTYPE, "activate={true, nocompatibility}", "final", "tracking=true", "kerning=true", "spacing=true", "factor=1100", "stretch=8", "shrink=8")
//                    .add(PackageName.TIKZ)
//                    .add(PackageName.HYPERREF, "unicode");
//
//            GenericCommand usetikzlibrary = new GenericCommand.Builder(CommandName.USETIKZLIBRARY.getString())
//                    .body(
//                            "positioning",
//                            "math",
//                            "colorbrewer",
//                            "backgrounds",
//                            "matrix")
//                    .bodyTerminator(Delimiter.COMMA)
//                    .build();
//            GenericCommand standaloneenv = new GenericCommand.Builder(CommandName.STANDALONEENV.getString())
//                    .body("tikzpicture")
//                    .build();


        } finally {
            datasource2.closeConnection();
        }

    }
}

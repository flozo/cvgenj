package de.flozo.cvgen;

import de.flozo.common.dto.appearance.*;
import de.flozo.common.dto.content.*;
import de.flozo.common.dto.latex.DocumentClass;
import de.flozo.common.dto.latex.LatexPackage;
import de.flozo.common.dto.latex.TikzLibrary;
import de.flozo.db.*;
import de.flozo.latex.assembly.IconCommand;
import de.flozo.latex.assembly.LayerList;
import de.flozo.latex.assembly.PackageList;
import de.flozo.latex.assembly.Preamble;
import de.flozo.latex.core.*;
import de.flozo.latex.tikz.MatrixOfNodes;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class Main {

    // constants
    public static final String APPLICATION_NAME = "cvgen";
    public static final String VERSION_NUMBER = "0.3";
    public static final String VERSION_DATE = "2022-08-11";

    public static final String REPO_URL = String.format("https://github.com/flozo/%1$s",
            APPLICATION_NAME);
    public static final String VERSION_INFO_LATEX_HEADER = String.format("%% =====  LaTeX code generated by %1$s v%2$s (%3$s)\n%% =====  %1$s by flozo (%4$s)",
            APPLICATION_NAME, VERSION_NUMBER, VERSION_DATE, REPO_URL);
    public static final String VERSION_INFO_PDF_META_DATA = String.format("%1$s v%2$s (%3$s); visit %4$s",
            APPLICATION_NAME, VERSION_NUMBER, VERSION_DATE, REPO_URL);


    public static void main(String[] args) {


        Datasource2 datasource2 = Datasource2.INSTANCE;
        Connection connection = datasource2.getConnection();

        try {

            LetterContentDAO letterContentDAO = new LetterContentDAOImpl(datasource2, connection);
            LetterContent letterContent = letterContentDAO.get("test");

            System.out.println(letterContent);

            Address receiver = letterContent.getReceiver();
            Address sender = letterContent.getSender();
            System.out.println(sender);
            System.out.println(receiver);

            TextItemDAO textItemDAO = new TextItemDAOImpl(datasource2, connection);

            ContentElement.Builder receiverNameLineBuilder = new ContentElement.Builder();
            if (receiver.getCompany() != null && !receiver.getCompany().isBlank()) {
                receiverNameLineBuilder.addComponent(receiver.getCompany());
            } else {
                receiverNameLineBuilder
                        .addComponent(receiver.getPerson().getFirstName())
                        .addComponent(receiver.getPerson().getLastName());
            }
            ContentElement receiverNameLine = receiverNameLineBuilder.inlineDelimiter(Delimiter.SPACE).build();
            ContentElement receiverStreetLine = new ContentElement.Builder()
                    .addComponent(receiver.getStreet())
                    .addComponent(receiver.getHouseNumber())
                    .inlineDelimiter(Delimiter.SPACE)
                    .build();
            ContentElement receiverCityLine = new ContentElement.Builder()
                    .addComponent(receiver.getPostalCode())
                    .addComponent(receiver.getCity())
                    .inlineDelimiter(Delimiter.SPACE)
                    .build();
            ContentElement addressFieldContent = new ContentElement.Builder()
                    .addComponent(receiverNameLine.getInline())
                    .addComponent(receiverStreetLine.getInline())
                    .addComponent(receiverCityLine.getInline())
                    .multilineContent(true)
                    .build();


            ContentElement senderNameLine = new ContentElement.Builder()
                    .addComponent(sender.getPerson().getFirstName())
                    .addComponent(sender.getPerson().getLastName())
                    .inlineDelimiter(Delimiter.SPACE)
                    .build();
            ContentElement senderNameLineWithTitle = new ContentElement.Builder()
                    .addComponent(sender.getPerson().getAcademicTitle() + ".")
                    .addComponent(sender.getPerson().getFirstName())
                    .addComponent(sender.getPerson().getLastName())
                    .inlineDelimiter(Delimiter.NON_BREAKING_SPACE)
                    .build();


            ContentElement senderStreetLine = new ContentElement.Builder()
                    .addComponent(sender.getStreet())
                    .addComponent(sender.getHouseNumber())
                    .inlineDelimiter(Delimiter.SPACE)
                    .build();
            ContentElement senderCityLine = new ContentElement.Builder()
                    .addComponent(sender.getPostalCode())
                    .addComponent(sender.getCity())
                    .inlineDelimiter(Delimiter.SPACE)
                    .build();
            ContentElement senderAddress = new ContentElement.Builder()
                    .addComponent(senderStreetLine.getInline())
                    .addComponent(senderCityLine.getInline())
                    .inlineDelimiter(Delimiter.DOUBLE_BACKSLASH)
                    .build();

            String backaddressSeparator = textItemDAO.get("backaddress_separator").getValue();
            ContentElement backaddressFieldContent = new ContentElement.Builder()
                    .addComponent(senderNameLine.getInline())
                    .addComponent(senderStreetLine.getInline())
                    .addComponent(senderCityLine.getInline())
                    .inlineDelimiter(backaddressSeparator)
                    .build();

            ContentElement dateFieldContent = new ContentElement.Builder()
                    .addComponent(sender.getCity())
                    .addComponent(letterContent.getDate())
                    .insertSpaceAfterDelimiter(true)
                    .inlineDelimiter(Delimiter.COMMA)
                    .build();

            ContentElement subjectFieldContent = new ContentElement.Builder()
                    .addComponent(letterContent.getSubject())
                    .build();

            ContentElement bodyContent = new ContentElement.Builder()
                    .addComponent(letterContent.getBodyText())
                    .build();

            EnclosureDAO enclosureDAO = new EnclosureDAOImpl(datasource2, connection);
            List<Enclosure> enclosureList = enclosureDAO.getAllIncluded();

            ContentElement enclosures = new ContentElement.Builder()
                    .addComponents(enclosureList.stream().map(Enclosure::getCaption).collect(Collectors.toList()))
                    .inlineDelimiter(Delimiter.COMMA)
                    .insertSpaceAfterDelimiter(true)
                    .build();
            ContentElement enclosureLine = new ContentElement.Builder()
                    .addComponent("Enclosures: ")
                    .addComponent(enclosures.getInline())
                    .build();

            ContentElement valedictionLine = new ContentElement.Builder()
                    .addComponent(letterContent.getValediction().getValue())
                    .build();

            ElementDAO elementDAO = new ElementDAOImpl(datasource2, connection);


            DocumentElement addressField = new DocumentElement("receiver_address", addressFieldContent, elementDAO.get("address"));
            DocumentElement backaddressField = new DocumentElement("backaddress", backaddressFieldContent, elementDAO.get("backaddress"));
            DocumentElement dateField = new DocumentElement("letter_date", dateFieldContent, elementDAO.get("date"));
            DocumentElement subjectField = new DocumentElement("letter_subject", subjectFieldContent, elementDAO.get("subject"));
            DocumentElement bodyField = new DocumentElement("letter_body", bodyContent, elementDAO.get("body"));
            DocumentElement enclosureTagLine = new DocumentElement("enclosures", enclosureLine, elementDAO.get("enclosures"));
            DocumentElement headline = new DocumentElement("headline", senderNameLineWithTitle, elementDAO.get("headline_field"));
            DocumentElement valediction = new DocumentElement("valediction", valedictionLine, elementDAO.get("valediction"));


            PageDAO pageDAO = new PageDAOImpl(datasource2, connection);
            Page letterPage = pageDAO.get("cv_motivational_letter");

            LineDAO lineDAO = new LineDAOImpl(datasource2, connection);
            List<Line> lineList = lineDAO.getAll();
            lineList.remove(0);


            EmbeddedFileDAO embeddedFileDAO = new EmbeddedFileDAOImpl(datasource2, connection);

            // Signature
            EmbeddedFile signatureFile = embeddedFileDAO.get("signature");
            String absoluteFilePathSignature = signatureFile.getFile().getPath().replaceFirst("^~", System.getProperty("user.home"));
            ContentElement signatureOption = new ContentElement.Builder()
                    .addComponent("scale")
                    .addComponent(String.valueOf(signatureFile.getScaleFactor()))
                    .inlineDelimiter(Delimiter.EQUALS)
                    .build();
            Command includeSignature = new GenericCommand.Builder("includegraphics")
                    .optionList(signatureOption.getInline())
                    .body(absoluteFilePathSignature)
                    .build();
            ContentElement includegraphicsSignature = new ContentElement.Builder(includeSignature.getInline())
                    .addComponent(senderNameLine.getInline())
                    .inlineDelimiter(Delimiter.DOUBLE_BACKSLASH)
                    .build();
            DocumentElement signature = new DocumentElement("signature", includegraphicsSignature, elementDAO.get("signature_letter"));

            // Photo
            EmbeddedFile photoFile = embeddedFileDAO.get("photo");
            String absoluteFilePathPhoto = photoFile.getFile().getPath().replaceFirst("^~", System.getProperty("user.home"));
            ContentElement photoOption = new ContentElement.Builder()
                    .addComponent("scale")
                    .addComponent(String.valueOf(photoFile.getScaleFactor()))
                    .inlineDelimiter(Delimiter.EQUALS)
                    .build();
            Command includePhoto = new GenericCommand.Builder("includegraphics")
                    .optionList(photoOption.getInline())
                    .body(absoluteFilePathPhoto)
                    .build();
            ContentElement includegraphicsPhoto = new ContentElement.Builder(includePhoto.getInline()).build();
            DocumentElement photo = new DocumentElement("photo", includegraphicsPhoto, elementDAO.get("cv_photo"));


            // Icons
            IconDAO iconDAO = new IconDAOImpl(datasource2, connection);
            IconCommand mapMarkerIcon = IconCommand.fromIcon(iconDAO.get("address"));
            IconCommand phoneIcon = IconCommand.fromIcon(iconDAO.get("phone"));
            IconCommand mailIcon = IconCommand.fromIcon(iconDAO.get("mail"));
            IconCommand githubIcon = IconCommand.fromIcon(iconDAO.get("github"));
            IconCommand hyperlink = IconCommand.fromIcon(iconDAO.get("hyperlink"));

            Element senderStyle = elementDAO.get("sender");
            Element senderStyleColumn1 = elementDAO.get("sender_column1");
            Element senderStyleColumn2 = elementDAO.get("sender_column2");

            ColumnStyle column1 = new ColumnStyle(senderStyleColumn1);
            ColumnStyle column2 = new ColumnStyle(senderStyleColumn2);

            ContentElement hyperlinkedEmailAddress = new ContentElement.Builder()
                    .addComponent(sender.getEMailAddress())
                    .makeHyperlink(sender.getEMailAddress(), subjectFieldContent.getInline())
                    .build();

            MatrixOfNodes senderField = new MatrixOfNodes.Builder("sender_field", senderStyle)
                    .addRow(senderAddress.getInline(), mapMarkerIcon.getInline())
                    .addRow(sender.getMobileNumber(), phoneIcon.getInline())
                    .addRow(hyperlinkedEmailAddress.getInline(), mailIcon.getInline())
                    .addColumnStyle(column1.getStyle())
                    .addColumnStyle(column2.getStyle())
                    .build();


            DocumentPage motivationalLetter = new DocumentPage.Builder("letter", letterPage)
                    .addElement(headline, addressField, backaddressField, dateField, subjectField, bodyField, enclosureTagLine)
                    .addMatrix(senderField)
                    .addElement(valediction)
                    .addElement(signature)
                    .addLine(lineList)
                    .insertLatexComments(true)
                    .build();


            ItemizeStyleDAO itemizeStyleDAO = new ItemizeStyleDAOImpl(datasource2, connection);
            ItemizeStyle itemizeStyle = itemizeStyleDAO.get("cv_blue_bullet");
            List<String> itemizeOptions = new ArrayList<>();
            itemizeOptions.add(String.format("topsep=%s", LengthExpression.fromLength(itemizeStyle.getTopSep()).getFormatted()));
            itemizeOptions.add(String.format("leftmargin=%s", LengthExpression.fromLength(itemizeStyle.getLeftMargin()).getFormatted()));
            itemizeOptions.add(String.format("labelsep=%s", LengthExpression.fromLength(itemizeStyle.getLabelSep()).getFormatted()));
            itemizeOptions.add(String.format("itemindent=%s", LengthExpression.fromLength(itemizeStyle.getItemIndent()).getFormatted()));
            itemizeOptions.add(String.format("itemsep=%s", LengthExpression.fromLength(itemizeStyle.getItemSep()).getFormatted()));
            itemizeOptions.add(String.format("label=%s", itemizeStyle.getLabel().getValue()));


            TimelineItemDAO timelineItemDAO = new TimelineItemDAOImpl(datasource2, connection);
            List<TimelineItem> educationTimeline = timelineItemDAO.getAllIncludedOfType("education");


            List<TimelineTextItemLink> wissMAItemList = timelineItemDAO.getTextItems("wissMA");
            List<TimelineTextItemLink> shkItemList = timelineItemDAO.getTextItems("SHK");

            List<String> wissMAItems = wissMAItemList.stream()
                    .map(TimelineTextItemLink::getTextItem)
                    .map(TextItem::getValue)
                    .collect(Collectors.toList());
            ItemizeEnvironment itemizeEnvironmentWissMA = new ItemizeEnvironment(itemizeOptions, wissMAItems);

            List<String> shkItems = shkItemList.stream()
                    .map(TimelineTextItemLink::getTextItem)
                    .map(TextItem::getValue)
                    .collect(Collectors.toList());
            ItemizeEnvironment itemizeEnvironmentSHK = new ItemizeEnvironment(itemizeOptions, shkItems);

            // career
            List<Element> styles = new ArrayList<>();
            styles.add(elementDAO.get("cv_date_column"));
            styles.add(elementDAO.get("cv_timeline_column2"));
            styles.add(elementDAO.get("cv_timeline_column3"));
            Timeline careerTimeline = new Timeline("career",
                    textItemDAO.get("cv_career_title"),
                    elementDAO.get("cv_career_title"),
                    timelineItemDAO.getAllIncludedOfType("career"),
                    elementDAO.get("cv_career"),
                    styles
            );


            Element cvContactStyleColumn1 = elementDAO.get("cv_contact_column1");
            Element cvContactStyleColumn2 = elementDAO.get("cv_contact_column2");

            ContentElement githubUrl = new ContentElement.Builder()
                    .addComponent(textItemDAO.get("github_url").getValue())
                    .addComponent("\\scriptsize" + hyperlink.getInline())
                    .makeHyperlink(textItemDAO.get("github_url").getValue())
                    .inlineDelimiter(Delimiter.SPACE.getString())
                    .build();

            // contact
            Element cvContactStyle = elementDAO.get("cv_contact");
            ContentElement cvContactTitle = new ContentElement.Builder()
                    .addComponent(textItemDAO.get("cv_contact_title").getValue())
                    .build();
            DocumentElement cvContactTitleField = new DocumentElement("cv_contact_title", cvContactTitle, elementDAO.get("cv_contact_title"));
            ColumnStyle cvContactColumn1 = new ColumnStyle(cvContactStyleColumn1);
            ColumnStyle cvContactColumn2 = new ColumnStyle(cvContactStyleColumn2);
            MatrixOfNodes cvContact = new MatrixOfNodes.Builder("cv_contact", cvContactStyle)
                    .addRow(mapMarkerIcon.getInline(), senderAddress.getInline())
                    .addRow(phoneIcon.getInline(), sender.getMobileNumber())
                    .addRow(mailIcon.getInline(), hyperlinkedEmailAddress.getInline())
                    .addRow(LengthExpression.inCentimeters(0.5), githubIcon.getInline(), githubUrl.getInline())
                    .addColumnStyle(cvContactColumn1.getStyle())
                    .addColumnStyle(cvContactColumn2.getStyle())
                    .build();


            ContentElement personalText = new ContentElement.Builder()
                    .addComponent("Geboren am")
                    .addComponent(sender.getPerson().getDateOfBirth())
                    .addComponent("in")
                    .addComponent(sender.getPerson().getPlaceOfBirth())
                    .inlineDelimiter(Delimiter.SPACE)
                    .finalDelimiter(Delimiter.COMMA)
                    .build();
            ContentElement maritalStatus = new ContentElement.Builder()
                    .addComponent(sender.getPerson().getNationality())
                    .addComponent(sender.getPerson().getMaritalStatus())
                    .addComponent(sender.getPerson().getChildren())
                    .inlineDelimiter(Delimiter.COMMA)
                    .insertSpaceAfterDelimiter(true)
                    .build();
            Element cvPersonalStyle = elementDAO.get("cv_personal");
            MatrixOfNodes cvPersonal = new MatrixOfNodes.Builder("cv_personal", cvPersonalStyle)
                    .addRow("", personalText.getInline())
                    .addRow("", maritalStatus.getInline())
                    .addColumnStyle(cvContactColumn1.getStyle())
                    .addColumnStyle(cvContactColumn2.getStyle())
                    .build();

            // personal title
            ContentElement cvPersonalTitle = new ContentElement.Builder()
                    .addComponent(textItemDAO.get("cv_personal_title").getValue())
                    .build();
            DocumentElement cvPersonalTitleField = new DocumentElement("cv_personal_title", cvPersonalTitle, elementDAO.get("cv_personal_title"));



//            Environment itemize = new Environment.Builder(EnvironmentName.ITEMIZE)
//                    .optionalArguments(itemizeOptions)
//                    .body(timelineItemListMap.get("edu_1"))
//                    .build();

            Page cvPage1 = pageDAO.get("cv_page_1");
            ContentElement cvTitle = new ContentElement.Builder()
                    .addComponent(textItemDAO.get("cv_title").getValue())
                    .build();

            DocumentElement cvTitleField = new DocumentElement("cv_title", cvTitle, elementDAO.get("cv_title"));

//
//
//            DocumentElement cvContactTitleField = new DocumentElement("cv_contact_title", cvContactTitle, elementDAO.get("cv_contact_title"));


            DocumentPage cv1 = new DocumentPage.Builder("cv1", cvPage1)
                    .addLine(lineDAO.get("headline_separation"))
                    .addElement(headline)
                    .addElement(cvTitleField)
                    .addElement(photo)
                    .addElement(cvContactTitleField)
                    .addMatrix(cvContact)
                    .addElement(cvPersonalTitleField)
                    .addMatrix(cvPersonal)
                    .addElement(careerTimeline.getTitleField())
                    .addMatrix(careerTimeline.getItemMatrix())
                    .insertLatexComments(true)
                    .build();


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
            hyperOptions.add(String.format("pdfauthor={%s}", senderNameLine.getInline()));
            hyperOptions.add(String.format("pdfdate={%s}", LocalDate.now()));
            hyperOptions.add(String.format("pdfproducer={%s}", VERSION_INFO_PDF_META_DATA));
            hyperOptions.add(String.format("pdfcontactcity={%s}", sender.getCity()));
            hyperOptions.add(String.format("pdfcontactcountry={%s}", sender.getCountry()));
            hyperOptions.add(String.format("pdfcontactemail={%s}", sender.getEMailAddress()));


            Preamble preamble = Preamble.create(documentClass, packageList, tikzLibraries, hyperOptions);

            LayerDAO layerDAO = new LayerDAOImpl(datasource2, connection);
            List<String> layers = layerDAO.getAll().stream().map(Layer::getName).collect(Collectors.toList());
            LayerList layerList = new LayerList.Builder(layers).build();
            List<String> layerDeclarationBlock = layerList.getLayerCode();


            ExpressionList documentBody = new FormattedExpressionList.Builder()
                    .append(layerDeclarationBlock)
                    .append(motivationalLetter.getCode())
                    .append(cv1.getCode())
                    .build();

            Environment document = new Environment.Builder(EnvironmentName.DOCUMENT)
                    .body(documentBody.getBlock())
                    .build();


            LatexCode laTeXCode = new LatexCode(VERSION_INFO_LATEX_HEADER, preamble, document);


            String fileName = "test_output.tex";
            String directory = "/tmp";

            OutputFile outputFile = new OutputFile(directory, fileName, laTeXCode.getCode());
            if (outputFile.create(true, true)) {
                System.out.println("[output] Done!");
            } else {
                System.out.println("[output] Something went wrong!");
            }


        } finally {
            datasource2.closeConnection();
        }

    }
}

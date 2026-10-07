package com.icit.rag.services;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Paths;

@Service
public class DocumentIngestionService {

    private static final Logger logger= LoggerFactory.getLogger(DocumentIngestionService.class);

    @Autowired
    @Qualifier("babyProductEmbeddingStore")
    private EmbeddingStore<TextSegment> babyProductEmbedding;
    @Autowired
    @Qualifier("vaccineEmbeddingStore")
    private EmbeddingStore<TextSegment> vaccineEmbedding;
    @Autowired
    private EmbeddingModel embeddingModel;

    @PostConstruct
    public void ingestAll(){
           ingestionPipeline("baby_products_catalog_100.pdf",babyProductEmbedding);
           ingestionPipeline("vaccine_dummy_catalog_200.pdf",vaccineEmbedding);
    }

    private void ingestionPipeline(String filename, EmbeddingStore<TextSegment> store) {
        try {
            URL resource = this.getClass().getClassLoader().getResource("docs/"+filename);
            if(resource == null){
                logger.error("Pdf {} not found", filename);
                return;
            }

            //Document loading
            Document document = FileSystemDocumentLoader.loadDocument(Paths.get(resource.toURI()), new ApachePdfBoxDocumentParser());

            //Ingestor with the help of
            EmbeddingStoreIngestor ingestor = getIngestor(store);
            ingestor.ingest(document);
            logger.info("Ingested {}", filename);

        } catch (Exception exception) {
            logger.error("Exception occurred During document ingestion", exception);
        }
    }

    private EmbeddingStoreIngestor getIngestor(EmbeddingStore<TextSegment> store) {
        //Chunker/Splitter
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                 //Embedding API (Embedding Model [Mistral])
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build();
    }

}

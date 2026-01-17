package com.example.pixelpatrol.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String stagingUrl;
    private String productionUrl;


    // --- NEW RELATIONSHIP ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    @JsonIgnore // Prevent infinite loops in JSON
    private Collection collection;

    // Standard Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStagingUrl() { return stagingUrl; }
    public void setStagingUrl(String stagingUrl) { this.stagingUrl = stagingUrl; }
    public String getProductionUrl() { return productionUrl; }
    public void setProductionUrl(String productionUrl) { this.productionUrl = productionUrl; }


    public Collection getCollection() { return collection; }
    public void setCollection(Collection collection) { this.collection = collection; }
}
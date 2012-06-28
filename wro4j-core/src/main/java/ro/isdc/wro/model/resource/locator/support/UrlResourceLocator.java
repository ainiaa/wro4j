/*
 * Copyright (C) 2011 Betfair.
 * All rights reserved.
 */
package ro.isdc.wro.model.resource.locator.support;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.model.resource.locator.ResourceLocator;

/**
 * UrlResourceLocator capable to read the resources from some URL. Usually, this uriLocator will be the last in the chain of
 * uriLocators.
 *
 * @author Alex Objelean
 * @created 30 Mar 2011
 * @since 1.4.0
 */
public class UrlResourceLocator
    extends AbstractResourceLocator {
  private static final Logger LOG = LoggerFactory.getLogger(UrlResourceLocator.class);
  public static final String ALIAS = "uri";
  /**
   * Path of the resource to be located.
   */
  private final String path;

  /**
   * Create a new UrlResource.
   * @param path a URL path
   * @throws MalformedURLException if the given URL path is not valid
   */
  public UrlResourceLocator(final String path) {
    if (path == null) {
      throw new IllegalArgumentException("path cannot be NULL!");
    }
    this.path = path;
  }

  public UrlResourceLocator(final URL url) {
    if (url == null) {
      throw new IllegalArgumentException("url cannot be NULL!");
    }
    this.path = url.toExternalForm();
  }

  /**
   * Check if a uri is a URL resource.
   *
   * @param uri to check.
   * @return true if the uri is a URL resource.
   */
  public static boolean isValid(final String uri) {
    // if creation of URL object doesn't throw an exception, the uri can be
    // accepted.
    try {
      new URL(uri);
    } catch (final MalformedURLException e) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getInputStream()
      throws IOException {
    LOG.debug("Reading path: " + path);
    if (getWildcardStreamLocator().hasWildcard(path)) {
      final String fullPath = FilenameUtils.getFullPath(path);
      final URL url = new URL(fullPath);
      return getWildcardStreamLocator().locateStream(path, new File(url.getFile()));
    }
    final URL url = new URL(path);
    final URLConnection con = url.openConnection();
    // sets the "UseCaches" flag to <code>false</code>, mainly to avoid jar file locking on Windows.
    con.setUseCaches(false);
    return new BufferedInputStream(con.getInputStream());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long lastModified() {
    try {
      final URL url = new URL(path);
      final File file = FileUtils.toFile(url);
      return file != null ? file.lastModified() : super.lastModified();
    } catch (final MalformedURLException e) {
      return super.lastModified();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResourceLocator createRelative(String relativePath)
      throws IOException {
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    return new UrlResourceLocator(new URL(new URL(this.path), relativePath));
  }
}

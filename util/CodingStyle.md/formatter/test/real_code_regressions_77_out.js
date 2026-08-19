/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

var express = require('../')
  , request = require('supertest');

it( 'should include ETag', function (done) {
    var app = createApp( path.resolve(fixtures, 'name.txt') );

  request(app)
  .get('/')
  .expect('ETag', /^(?:W\/)?"[^"]+"$/)
  .expect(200, 'tobi', done);
} );

/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Faces {

    List<Person> characters()
    {
        List<Person> out = new java.util.ArrayList<>();
        for( Concept c : concepts.all() ) {
            if( CHARACTER_CATEGORY.equals(c.categoryId) ) {
                String id = String.valueOf(c.conceptId);
                int samples = animeTask instanceof AnimeFaceTask ? ( (AnimeFaceTask) animeTask ).sampleCount(
                    id
                ) : 0;
                out.add( new Person(c.conceptId, samples) );
            } // if
        } // for

        return out;
    }

    static double[] alignment(Face f)
    {
        double[][] src = { { f.kx(0), f.ky(0) }, { f.kx(1), f.ky(1) }, { f.kx(2), f.ky(2) }, { f.kx(3), f.ky(3) } };

        return FaceAligner.fourPoint(src);
    }

} // class Faces

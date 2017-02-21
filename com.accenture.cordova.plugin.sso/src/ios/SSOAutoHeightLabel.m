//
//  SSOAutoHeightLabel.m
//  Accenture SSO
//
//  Created by Joshua Lamkin on 10/7/15.
//  Copyright © 2015 Accenture. All rights reserved.
//

#import "SSOAutoHeightLabel.h"

@implementation SSOAutoHeightLabel

- (void)setBounds:(CGRect)bounds {
    if (bounds.size.width != self.bounds.size.width) {
        [self setNeedsUpdateConstraints];
    }
    [super setBounds:bounds];
}

- (void)updateConstraints {
    if (self.preferredMaxLayoutWidth != self.bounds.size.width) {
        self.preferredMaxLayoutWidth = self.bounds.size.width;
    }
    [super updateConstraints];
}


@end
